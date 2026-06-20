using System;
using System.Globalization;
using System.Threading;
using System.Windows.Forms;

namespace RuntimeVisor
{
    internal static class Program
    {
        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [STAThread]
        static void Main(string[] args)
        {
            ApplyCulture(args);
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new Form1());
        }

        /// <summary>
        /// Idioma de la interfaz: por defecto el del sistema operativo (con español
        /// como base si no hay satélite que matchee). Se puede forzar con
        /// <c>--lang=es|en</c> o la variable de entorno <c>BBK_LANG</c>, útil para
        /// probar el inglés sin cambiar la configuración de Windows.
        /// </summary>
        private static void ApplyCulture(string[] args)
        {
            string lang = null;
            foreach (string a in args)
            {
                if (a != null && a.StartsWith("--lang=", StringComparison.OrdinalIgnoreCase))
                {
                    lang = a.Substring("--lang=".Length);
                }
            }
            if (string.IsNullOrEmpty(lang))
            {
                lang = Environment.GetEnvironmentVariable("BBK_LANG");
            }
            if (string.IsNullOrEmpty(lang)) return;

            try
            {
                CultureInfo culture = CultureInfo.GetCultureInfo(lang);
                CultureInfo.DefaultThreadCurrentUICulture = culture;
                Thread.CurrentThread.CurrentUICulture = culture;
            }
            catch (CultureNotFoundException)
            {
                // idioma desconocido -> se queda con el del SO.
            }
        }
    }
}
