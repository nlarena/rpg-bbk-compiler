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
        public static string MenuOption2 => Get();
        public static string MenuOption3 => Get();
        public static string MenuPrompt => Get();
        public static string MenuFkeys => Get();
        public static string MenuUnknownCommand => Get();

        // --- Crear archivo físico (FilesView) ---
        public static string FilesTitle => Get();
        public static string FilesFile => Get();
        public static string FilesLibrary => Get();
        public static string FilesText => Get();
        public static string FilesColField => Get();
        public static string FilesColType => Get();
        public static string FilesColLength => Get();
        public static string FilesColDecimals => Get();
        public static string FilesColKey => Get();
        public static string FilesFkeys => Get();
        public static string FilesHint => Get();
        public static string FilesNeedName => Get();
        public static string FilesNeedFields => Get();
        public static string FilesCreating => Get();
        public static string FilesCreated => Get();
        public static string FilesEditTitle => Get();
        public static string FilesEditFkeys => Get();
        public static string FilesUpdated => Get();

        // --- SQL interactivo (SqlView / STRSQL) ---
        public static string SqlTitle => Get();
        public static string SqlPrompt => Get();
        public static string SqlFkeys => Get();
        public static string SqlHint => Get();
        public static string SqlRunning => Get();
        public static string SqlNoRows => Get();
        public static string SqlRowsOne => Get();
        public static string SqlRowsMany => Get();
        public static string SqlTruncated => Get();
        public static string SqlUpdated => Get();

        // --- Trabajar con archivos (FileListView / WRKF) ---
        public static string FileListTitle => Get();
        public static string FileListFkeys => Get();
        public static string FileListColFields => Get();
        public static string FileListColTable => Get();
        public static string FileListRecordFormat => Get();
        public static string FileListQuerying => Get();
        public static string FileListNone => Get();
        public static string FileListCountOne => Get();
        public static string FileListCountMany => Get();

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
