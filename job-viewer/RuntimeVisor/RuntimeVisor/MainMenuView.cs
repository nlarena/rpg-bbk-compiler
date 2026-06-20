using System;
using System.Windows.Forms;

namespace RuntimeVisor
{
    /// <summary>
    /// Menú principal estilo IBM i: opciones numeradas (clickeables) + línea de
    /// comando ('===>') donde se tipea el número de opción o el comando
    /// (p.ej. WRKACTJOB). La navegación se delega vía {@link Navigate}.
    /// </summary>
    public partial class MainMenuView : UserControl
    {
        /// <summary>Callback de navegación (lo setea el HomeForm): recibe el destino ("JOBS", "HOME", ...).</summary>
        public Action<string> Navigate { get; set; }

        public MainMenuView()
        {
            InitializeComponent();
            LocalizeTexts();
        }

        private void LocalizeTexts()
        {
            lblHeader.Text = Strings.MenuSelectOption;
            lblOption1.Text = Strings.MenuOption1;
            lblPrompt.Text = Strings.MenuPrompt;
            lblFkeys.Text = Strings.MenuFkeys;
        }

        private void lblOption1_Click(object sender, EventArgs e)
        {
            Navigate?.Invoke("JOBS");
        }

        private void txtCommand_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.KeyCode != Keys.Enter) return;
            e.SuppressKeyPress = true;

            string cmd = txtCommand.Text.Trim().ToUpperInvariant();
            txtCommand.Clear();
            lblStatus.Text = "";

            if (cmd.Length == 0) return;

            if (cmd == "1" || cmd == "WRKACTJOB")
            {
                Navigate?.Invoke("JOBS");
            }
            else
            {
                lblStatus.Text = Strings.Format(Strings.MenuUnknownCommand, cmd);
            }
        }
    }
}
