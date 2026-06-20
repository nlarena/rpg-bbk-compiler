using System;
using System.Drawing;
using System.Windows.Forms;

namespace RuntimeVisor
{
    /// <summary>
    /// Pantalla principal del runtime. Arriba, una cabecera estilo consola 5250
    /// (Sistema / Subsistema / Terminal / Usuario / Fecha / Hora); el cuerpo va
    /// vacío por ahora (ahí irá la grilla de jobs, etc.).
    /// </summary>
    public partial class HomeForm : Form
    {
        // TODO: Sistema y Subsistema son placeholders. Cuando el runtime exponga
        // info de sistema (p.ej. GET /api/system), traerlos de ahí.
        private const string SystemName = "BOXBREAK";
        private const string Subsystem = "QINTER";

        private string _currentView;

        public HomeForm()
        {
            InitializeComponent();
            clockTimer.Start();
            NavigateTo("HOME");
        }

        /// <summary>Cambia el contenido del cuerpo; la cabecera 5250 queda fija.</summary>
        public void NavigateTo(string target)
        {
            UserControl view;
            switch (target)
            {
                case "JOBS":
                    JobsView jobs = new JobsView();
                    jobs.Navigate = NavigateTo;
                    view = jobs;
                    break;
                case "FILES":
                    FilesView filesView = new FilesView();
                    filesView.Navigate = NavigateTo;
                    view = filesView;
                    break;
                case "FILELIST":
                    FileListView fileList = new FileListView();
                    fileList.Navigate = NavigateTo;
                    fileList.EditFile = OpenFileEditor;
                    view = fileList;
                    break;
                case "SQL":
                    SqlView sqlView = new SqlView();
                    sqlView.Navigate = NavigateTo;
                    view = sqlView;
                    break;
                case "EXIT":
                    Close();
                    return;
                default:
                    target = "HOME";
                    MainMenuView menu = new MainMenuView();
                    menu.Navigate = NavigateTo;
                    view = menu;
                    break;
            }
            _currentView = target;
            ApplyHeaderMode(target != "HOME");
            ShowView(view);
        }

        /// <summary>Abre la pantalla de archivo en modo edición (CHGPF) sobre el archivo dado.</summary>
        private void OpenFileEditor(FileDto file)
        {
            FilesView editor = new FilesView();
            editor.Navigate = NavigateTo;
            editor.LoadForEdit(file);
            _currentView = "FILES";
            ApplyHeaderMode(true);
            ShowView(editor);
        }

        private void ShowView(UserControl view)
        {
            pnlBody.SuspendLayout();
            Control[] existing = new Control[pnlBody.Controls.Count];
            pnlBody.Controls.CopyTo(existing, 0);
            pnlBody.Controls.Clear();
            foreach (Control c in existing) c.Dispose();
            view.Dock = DockStyle.Fill;
            pnlBody.Controls.Add(view);
            pnlBody.ResumeLayout();
            view.Focus();
        }

        // F3: en una sub-vista vuelve al menú; en el menú, sale.
        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (keyData == Keys.F3)
            {
                if (_currentView == "HOME") { Close(); } else { NavigateTo("HOME"); }
                return true;
            }
            return base.ProcessCmdKey(ref msg, keyData);
        }

        private void clockTimer_Tick(object sender, EventArgs e)
        {
            UpdateHeader();
        }

        private void UpdateHeader()
        {
            DateTime now = DateTime.Now;
            string user = string.IsNullOrEmpty(RuntimeSession.User) ? "-" : RuntimeSession.User;
            string nl = Environment.NewLine;

            if (_currentView == "HOME" || _currentView == null)
            {
                // header completo: sólo en el Home
                lblHeaderInfo.Text =
                    Strings.HeaderSystem + "  " + SystemName + nl +
                    Strings.HeaderSubsystem + "  " + Subsystem + nl +
                    Strings.HeaderTerminal + "  " + Environment.MachineName + nl +
                    Strings.HeaderUser + "  " + user + nl +
                    Strings.HeaderDate + "  " + now.ToString("dd/MM/yyyy") + nl +
                    Strings.HeaderTime + "  " + now.ToString("HH:mm:ss");
            }
            else
            {
                // header resumido: en las sub-vistas (como una pantalla de trabajo 5250)
                lblHeaderInfo.Text =
                    Strings.HeaderSystemShort + " " + SystemName + "    " + Strings.HeaderSubsystemShort + " " + Subsystem + "    " + Strings.HeaderUserShort + " " + user + nl +
                    Strings.HeaderTerminalShort + " " + Environment.MachineName + "    " + now.ToString("dd/MM/yyyy") + "  " + now.ToString("HH:mm:ss");
            }
        }

        /// <summary>Header completo en el Home; resumido (2 líneas) en las sub-vistas.</summary>
        private void ApplyHeaderMode(bool compact)
        {
            if (compact)
            {
                lblTitle.Font = new Font("Consolas", 12F, FontStyle.Bold);
                lblHeaderInfo.Location = new Point(22, 46);
                pnlHeader.Height = 94;
            }
            else
            {
                lblTitle.Font = new Font("Consolas", 17F, FontStyle.Bold);
                lblHeaderInfo.Location = new Point(22, 58);
                pnlHeader.Height = 205;
            }
            UpdateHeader();
        }
    }
}
