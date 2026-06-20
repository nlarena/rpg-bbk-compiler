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
    }
}
