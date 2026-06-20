using System;
using System.Collections.Generic;
using System.Drawing;
using System.Globalization;
using System.Windows.Forms;

namespace RuntimeVisor
{
    /// <summary>
    /// Vista de trabajos activos (análoga a WRKACTJOB): consulta GET /api/jobs y
    /// puebla la grilla con los trabajos del runtime, más recientes primero.
    /// F5 refresca; F3 vuelve al menú (lo maneja el HomeForm).
    /// </summary>
    public partial class JobsView : UserControl
    {
        private readonly RuntimeApiClient _api = new RuntimeApiClient();

        public Action<string> Navigate { get; set; }

        public JobsView()
        {
            InitializeComponent();
            LocalizeTexts();
            SetupGrid();
            LoadJobs();
        }

        private void LocalizeTexts()
        {
            lblTitle.Text = Strings.JobsTitle;
            lblFkeys.Text = Strings.JobsFkeys;
            lblNote.Text = Strings.JobsQuerying;
        }

        // F5 refresca la grilla; F3 lo maneja el HomeForm (vuelve al menú).
        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (keyData == Keys.F5)
            {
                LoadJobs();
                return true;
            }
            return base.ProcessCmdKey(ref msg, keyData);
        }

        private async void LoadJobs()
        {
            lblNote.ForeColor = Color.FromArgb(230, 200, 110);
            lblNote.Text = Strings.JobsQuerying;
            try
            {
                List<JobDto> jobs = await _api.GetJobsAsync();
                grid.Rows.Clear();
                foreach (JobDto j in jobs)
                {
                    grid.Rows.Add(j.jobNumber, j.user, j.name, StatusLabel(j.status),
                        FormatTime(j.createdAt), FormatTime(j.endedAt));
                }
                lblNote.ForeColor = Color.FromArgb(120, 200, 120);
                lblNote.Text = jobs.Count == 0
                    ? Strings.JobsNone
                    : Strings.Format(jobs.Count == 1 ? Strings.JobsCountOne : Strings.JobsCountMany, jobs.Count);
            }
            catch (RuntimeApiException ex)
            {
                grid.Rows.Clear();
                lblNote.ForeColor = Color.FromArgb(235, 130, 120);
                lblNote.Text = ex.Message;
            }
            catch (Exception)
            {
                grid.Rows.Clear();
                lblNote.ForeColor = Color.FromArgb(235, 130, 120);
                lblNote.Text = Strings.JobsConnectError;
            }
        }

        // El estado (ACTIVE/ENDED) es dato del dominio: no se traduce, sólo se normaliza.
        private static string StatusLabel(string status)
        {
            return string.IsNullOrEmpty(status) ? "" : status.ToUpperInvariant();
        }

        // Las marcas de tiempo llegan en ISO-8601 UTC; las mostramos en hora local.
        private static string FormatTime(string iso)
        {
            if (string.IsNullOrEmpty(iso)) return "";
            DateTimeOffset parsed;
            if (DateTimeOffset.TryParse(iso, CultureInfo.InvariantCulture,
                    DateTimeStyles.AssumeUniversal | DateTimeStyles.AdjustToUniversal, out parsed))
            {
                return parsed.ToLocalTime().ToString("dd/MM/yyyy HH:mm:ss");
            }
            return iso;
        }

        private void SetupGrid()
        {
            grid.AutoGenerateColumns = false;
            grid.ReadOnly = true;
            grid.AllowUserToAddRows = false;
            grid.AllowUserToResizeRows = false;
            grid.RowHeadersVisible = false;
            grid.MultiSelect = false;
            grid.SelectionMode = DataGridViewSelectionMode.FullRowSelect;
            grid.BorderStyle = BorderStyle.None;
            grid.EnableHeadersVisualStyles = false;
            grid.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.DisableResizing;
            grid.BackgroundColor = Color.FromArgb(12, 20, 12);
            grid.GridColor = Color.FromArgb(40, 60, 40);

            grid.ColumnHeadersDefaultCellStyle.BackColor = Color.FromArgb(20, 34, 20);
            grid.ColumnHeadersDefaultCellStyle.ForeColor = Color.FromArgb(150, 220, 150);
            grid.ColumnHeadersDefaultCellStyle.Font = new Font("Consolas", 9.5F, FontStyle.Bold);

            grid.DefaultCellStyle.BackColor = Color.FromArgb(12, 20, 12);
            grid.DefaultCellStyle.ForeColor = Color.FromArgb(180, 230, 180);
            grid.DefaultCellStyle.SelectionBackColor = Color.FromArgb(30, 70, 30);
            grid.DefaultCellStyle.SelectionForeColor = Color.FromArgb(210, 255, 210);
            grid.DefaultCellStyle.Font = new Font("Consolas", 9.5F);

            AddColumn(Strings.JobsColNumber, 90);
            AddColumn(Strings.JobsColUser, 110);
            AddColumn(Strings.JobsColName, 150);
            AddColumn(Strings.JobsColStatus, 90);
            AddColumn(Strings.JobsColCreated, 160);
            AddColumn(Strings.JobsColEnded, 160);
        }

        private void AddColumn(string header, int width)
        {
            grid.Columns.Add(new DataGridViewTextBoxColumn { HeaderText = header, Width = width });
        }
    }
}
