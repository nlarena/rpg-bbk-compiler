using System.Globalization;
using System.Resources;
using System.Runtime.CompilerServices;

namespace RuntimeVisor
{
    /// <summary>
    /// Acceso a los textos localizados del visor. El recurso neutro
    /// (<c>Resources/Strings.resx</c>) está en español; <c>Strings.en.resx</c>
    /// aporta el inglés. El idioma se resuelve por
    /// <see cref="CultureInfo.CurrentUICulture"/> (configurada en Program.cs).
    ///
    /// <para>Cada propiedad usa <see cref="CallerMemberNameAttribute"/>, así la
    /// clave del recurso es el propio nombre de la propiedad — sin literales que
    /// se desincronicen.</para>
    /// </summary>
    internal static class Strings
    {
        private static readonly ResourceManager Manager =
            new ResourceManager("RuntimeVisor.Resources.Strings", typeof(Strings).Assembly);

        private static string Get([CallerMemberName] string key = null)
        {
            return Manager.GetString(key, CultureInfo.CurrentUICulture) ?? key;
        }

        /// <summary>Formatea un patrón con marcadores: <c>Strings.Format(Strings.JobsCountMany, n)</c>.</summary>
        public static string Format(string pattern, params object[] args)
        {
            return string.Format(CultureInfo.CurrentCulture, pattern, args);
        }

        // --- Sign-on (Form1) ---
        public static string LoginWindowTitle => Get();
        public static string LoginUser => Get();
        public static string LoginPassword => Get();
        public static string LoginButton => Get();
        public static string LoginEnterCredentials => Get();
        public static string LoginConnecting => Get();
        public static string LoginConnectError => Get();

        // --- Menú principal (MainMenuView) ---
        public static string MenuSelectOption => Get();
        public static string MenuOption1 => Get();
        public static string MenuPrompt => Get();
        public static string MenuFkeys => Get();
        public static string MenuUnknownCommand => Get();

        // --- Cabecera 5250 (HomeForm) ---
        public static string HeaderSystem => Get();
        public static string HeaderSubsystem => Get();
        public static string HeaderTerminal => Get();
        public static string HeaderUser => Get();
        public static string HeaderDate => Get();
        public static string HeaderTime => Get();
        public static string HeaderSystemShort => Get();
        public static string HeaderSubsystemShort => Get();
        public static string HeaderUserShort => Get();
        public static string HeaderTerminalShort => Get();

        // --- Trabajos (JobsView) ---
        public static string JobsTitle => Get();
        public static string JobsFkeys => Get();
        public static string JobsColNumber => Get();
        public static string JobsColUser => Get();
        public static string JobsColName => Get();
        public static string JobsColStatus => Get();
        public static string JobsColCreated => Get();
        public static string JobsColEnded => Get();
        public static string JobsQuerying => Get();
        public static string JobsNone => Get();
        public static string JobsCountOne => Get();
        public static string JobsCountMany => Get();
        public static string JobsConnectError => Get();

        // --- Errores de la API (RuntimeApiClient) ---
        public static string ErrInvalidCredentials => Get();
        public static string ErrLoginRejected => Get();
        public static string ErrSessionExpired => Get();
        public static string ErrNoJobAuthority => Get();
        public static string ErrQueryRejected => Get();
    }
}
