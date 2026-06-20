using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Web.Script.Serialization;

namespace RuntimeVisor
{
    /// <summary>Respuesta del endpoint <c>POST /api/auth/login</c> del runtime.</summary>
    public class LoginResponse
    {
        public string token { get; set; }
        public string user { get; set; }
        public List<string> authorities { get; set; }
    }

    /// <summary>Un trabajo, tal como lo expone <c>GET /api/jobs</c> (análogo a WRKACTJOB).</summary>
    public class JobDto
    {
        public string jobNumber { get; set; }
        public string name { get; set; }
        public string user { get; set; }
        public string status { get; set; }
        public string createdAt { get; set; }   // ISO-8601 UTC, o null
        public string endedAt { get; set; }      // ISO-8601 UTC, o null
    }

    /// <summary>Un campo del formato de registro de un archivo (al declarar / listar).</summary>
    public class FileFieldDto
    {
        public string name { get; set; }
        public string type { get; set; }     // CHAR | DECIMAL | INTEGER | DATE
        public int length { get; set; }
        public int decimals { get; set; }
    }

    /// <summary>Un archivo de base de datos (physical file), tal como lo expone <c>/api/files</c>.</summary>
    public class FileDto
    {
        public string name { get; set; }
        public string library { get; set; }
        public string text { get; set; }
        public string tableName { get; set; }
        public string createdAt { get; set; }
        public List<FileFieldDto> fields { get; set; }
    }

    /// <summary>Resultado de una sentencia SQL (STRSQL): result set (RESULT) o conteo (UPDATE).</summary>
    public class SqlResultDto
    {
        public string kind { get; set; }              // RESULT | UPDATE
        public List<string> columns { get; set; }
        public List<List<string>> rows { get; set; }
        public int? updateCount { get; set; }
        public bool truncated { get; set; }
    }

    /// <summary>Cuerpo de error estándar de Spring Boot (con <c>server.error.include-message=always</c>).</summary>
    public class ApiErrorBody
    {
        public int status { get; set; }
        public string error { get; set; }
        public string message { get; set; }
        public string path { get; set; }
    }

    /// <summary>El runtime rechazó la operación (sign-on fallido, sin autoridad, etc.).</summary>
    public class RuntimeApiException : Exception
    {
        public int StatusCode { get; }

        public RuntimeApiException(int statusCode, string message) : base(message)
        {
            StatusCode = statusCode;
        }
    }

    /// <summary>Estado de la sesión autenticada: el JWT y la identidad del usuario.</summary>
    public static class RuntimeSession
    {
        public static string Token { get; private set; }
        public static string User { get; private set; }
        public static List<string> Authorities { get; private set; } = new List<string>();

        public static bool IsAuthenticated { get { return !string.IsNullOrEmpty(Token); } }

        public static void SignIn(LoginResponse login)
        {
            Token = login.token;
            User = login.user;
            Authorities = login.authorities ?? new List<string>();
        }

        public static void SignOut()
        {
            Token = null;
            User = null;
            Authorities = new List<string>();
        }
    }

    /// <summary>Cliente HTTP contra el runtime (servicio Spring Boot REST).</summary>
    public class RuntimeApiClient
    {
        private static readonly HttpClient Http =
            new HttpClient { BaseAddress = new Uri("http://localhost:8080/") };

        private readonly JavaScriptSerializer _json = new JavaScriptSerializer();

        /// <summary>Sign-on: POST de credenciales, devuelve el JWT y las autoridades.</summary>
        public async Task<LoginResponse> LoginAsync(string user, string password)
        {
            string payload = _json.Serialize(new { user = user, password = password });
            using (var content = new StringContent(payload, Encoding.UTF8, "application/json"))
            using (var response = await Http.PostAsync("api/auth/login", content))
            {
                string body = await response.Content.ReadAsStringAsync();
                if (!response.IsSuccessStatusCode)
                {
                    string message = response.StatusCode == HttpStatusCode.Unauthorized
                        ? Strings.ErrInvalidCredentials
                        : Strings.Format(Strings.ErrLoginRejected, (int)response.StatusCode);
                    throw new RuntimeApiException((int)response.StatusCode, message);
                }
                return _json.Deserialize<LoginResponse>(body);
            }
        }

        /// <summary>Lista los trabajos (GET /api/jobs), con el JWT de la sesión en el header.</summary>
        public async Task<List<JobDto>> GetJobsAsync()
        {
            using (var request = new HttpRequestMessage(HttpMethod.Get, "api/jobs"))
            {
                if (!string.IsNullOrEmpty(RuntimeSession.Token))
                {
                    request.Headers.Authorization =
                        new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", RuntimeSession.Token);
                }
                using (var response = await Http.SendAsync(request))
                {
                    string body = await response.Content.ReadAsStringAsync();
                    if (!response.IsSuccessStatusCode)
                    {
                        string message;
                        switch ((int)response.StatusCode)
                        {
                            case 401: message = Strings.ErrSessionExpired; break;
                            case 403: message = Strings.ErrNoJobAuthority; break;
                            default: message = Strings.Format(Strings.ErrQueryRejected, (int)response.StatusCode); break;
                        }
                        throw new RuntimeApiException((int)response.StatusCode, message);
                    }
                    return _json.Deserialize<List<JobDto>>(body) ?? new List<JobDto>();
                }
            }
        }

        /// <summary>Lista los archivos declarados (GET /api/files), con sus campos.</summary>
        public async Task<List<FileDto>> GetFilesAsync()
        {
            using (var request = new HttpRequestMessage(HttpMethod.Get, "api/files"))
            {
                if (!string.IsNullOrEmpty(RuntimeSession.Token))
                {
                    request.Headers.Authorization =
                        new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", RuntimeSession.Token);
                }
                using (var response = await Http.SendAsync(request))
                {
                    string body = await response.Content.ReadAsStringAsync();
                    if (!response.IsSuccessStatusCode)
                    {
                        throw new RuntimeApiException((int)response.StatusCode, ErrorMessage(body, (int)response.StatusCode));
                    }
                    return _json.Deserialize<List<FileDto>>(body) ?? new List<FileDto>();
                }
            }
        }

        /// <summary>Declara un archivo de base de datos (POST /api/files); crea la tabla física real.</summary>
        public async Task<FileDto> DeclareFileAsync(object request)
        {
            string payload = _json.Serialize(request);
            using (var req = new HttpRequestMessage(HttpMethod.Post, "api/files"))
            {
                if (!string.IsNullOrEmpty(RuntimeSession.Token))
                {
                    req.Headers.Authorization =
                        new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", RuntimeSession.Token);
                }
                req.Content = new StringContent(payload, Encoding.UTF8, "application/json");
                using (var response = await Http.SendAsync(req))
                {
                    string body = await response.Content.ReadAsStringAsync();
                    if (!response.IsSuccessStatusCode)
                    {
                        throw new RuntimeApiException((int)response.StatusCode, ErrorMessage(body, (int)response.StatusCode));
                    }
                    return _json.Deserialize<FileDto>(body);
                }
            }
        }

        /// <summary>Modifica el formato de registro de un archivo (PUT /api/files/{library}/{name}); aplica ALTER TABLE.</summary>
        public async Task<FileDto> ModifyFileAsync(string library, string name, object request)
        {
            string url = "api/files/" + Uri.EscapeDataString(library) + "/" + Uri.EscapeDataString(name);
            string payload = _json.Serialize(request);
            using (var req = new HttpRequestMessage(HttpMethod.Put, url))
            {
                if (!string.IsNullOrEmpty(RuntimeSession.Token))
                {
                    req.Headers.Authorization =
                        new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", RuntimeSession.Token);
                }
                req.Content = new StringContent(payload, Encoding.UTF8, "application/json");
                using (var response = await Http.SendAsync(req))
                {
                    string body = await response.Content.ReadAsStringAsync();
                    if (!response.IsSuccessStatusCode)
                    {
                        throw new RuntimeApiException((int)response.StatusCode, ErrorMessage(body, (int)response.StatusCode));
                    }
                    return _json.Deserialize<FileDto>(body);
                }
            }
        }

        /// <summary>Ejecuta una sentencia SQL (POST /api/sql) y devuelve el resultado.</summary>
        public async Task<SqlResultDto> RunSqlAsync(string sql)
        {
            string payload = _json.Serialize(new { sql = sql });
            using (var req = new HttpRequestMessage(HttpMethod.Post, "api/sql"))
            {
                if (!string.IsNullOrEmpty(RuntimeSession.Token))
                {
                    req.Headers.Authorization =
                        new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", RuntimeSession.Token);
                }
                req.Content = new StringContent(payload, Encoding.UTF8, "application/json");
                using (var response = await Http.SendAsync(req))
                {
                    string body = await response.Content.ReadAsStringAsync();
                    if (!response.IsSuccessStatusCode)
                    {
                        throw new RuntimeApiException((int)response.StatusCode, ErrorMessage(body, (int)response.StatusCode));
                    }
                    return _json.Deserialize<SqlResultDto>(body);
                }
            }
        }

        /// <summary>Saca el motivo real del cuerpo de error del runtime; si no hay, cae a un mensaje por status.</summary>
        private string ErrorMessage(string body, int statusCode)
        {
            try
            {
                ApiErrorBody err = _json.Deserialize<ApiErrorBody>(body);
                if (err != null && !string.IsNullOrWhiteSpace(err.message)) return err.message;
            }
            catch { /* cuerpo no-JSON: caemos al genérico */ }

            switch (statusCode)
            {
                case 401: return Strings.ErrSessionExpired;
                case 403: return "Sin autoridad *ALLOBJ para crear archivos.";
                default: return "El runtime rechazó la operación (HTTP " + statusCode + ").";
            }
        }
    }
}
