using System;
using System.Collections.Generic;
using System.Drawing;
using System.Globalization;
using System.Windows.Forms;

namespace RuntimeVisor
{
    /// <summary>
    /// Vista de archivos declarados (análoga a WRKF): consulta GET /api/files y
    /// lista los physical files; al seleccionar uno, el grid inferior muestra su
    /// formato de registro (los campos). F5 refresca; F3 vuelve (lo maneja el HomeForm).
    /// </summary>
    public partial class FileListView : UserControl
    {
        private readonly RuntimeApiClient _api = new RuntimeApiClient();
        private List<FileDto> _files = new List<FileDto>();

        public Action<string> Navigate { get; set; }

        /// <summary>Callback para abrir la edición de un archivo (lo setea el HomeForm).</summary>
        public Action<FileDto> EditFile { get; set; }

        public FileListView()
        {
            InitializeComponent();
            LocalizeTexts();
            SetupGrids();
            LoadFiles();
        }

        private void LocalizeTexts()
        {
            lblTitle.Text = Strings.FileListTitle;
            lblFkeys.Text = Strings.FileListFkeys;
            lblFmt.Text = Strings.FileListRecordFormat;
            lblNote.Text = Strings.FileListQuerying;
        }

        // F5 refresca; F6 crea (CRTPF); Intro cambia el seleccionado; F3 lo maneja el HomeForm.
        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (keyData == Keys.F5)
            {
                LoadFiles();
                return true;
            }
            if (keyData == Keys.F6)
            {
                Navigate?.Invoke("FILES");
                return true;
            }
            if (keyData == Keys.Enter && gridFiles.Focused)
            {
                EditSelected();
                return true;
            }
            return base.ProcessCmdKey(ref msg, keyData);
        }

        private void EditSelected()
        {
            int i = gridFiles.CurrentRow != null ? gridFiles.CurrentRow.Index : -1;
            if (i < 0 || i >= _files.Count) return;
            EditFile?.Invoke(_files[i]);
        }

        private void SetupGrids()
        {
            StyleGrid(gridFiles);
            gridFiles.SelectionChanged += (s, e) => ShowFieldsForSelection();
            gridFiles.CellDoubleClick += (s, e) => { if (e.RowIndex >= 0) EditSelected(); };
            AddColumn(gridFiles, Strings.FilesFile, 110);
            AddColumn(gridFiles, Strings.FilesLibrary, 110);
            AddColumn(gridFiles, Strings.FilesText, 200);
            AddColumn(gridFiles, Strings.FileListColFields, 70);
            AddColumn(gridFiles, Strings.FileListColTable, 160);
            AddColumn(gridFiles, Strings.JobsColCreated, 150);

            StyleGrid(gridFields);
            AddColumn(gridFields, Strings.FilesColField, 160);
            AddColumn(gridFields, Strings.FilesColType, 110);
            AddColumn(gridFields, Strings.FilesColLength, 90);
            AddColumn(gridFields, Strings.FilesColDecimals, 100);
            AddColumn(gridFields, Strings.FilesColKey, 70);
        }

        private async void LoadFiles()
        {
            lblNote.ForeColor = Color.FromArgb(230, 200, 110);
            lblNote.Text = Strings.FileListQuerying;
            try
            {
                _files = await _api.GetFilesAsync();
                gridFiles.Rows.Clear();
                gridFields.Rows.Clear();
                foreach (FileDto f in _files)
                {
                    int n = f.fields == null ? 0 : f.fields.Count;
                    gridFiles.Rows.Add(f.name, f.library, f.text, n, f.tableName, FormatTime(f.createdAt));
                }
                lblNote.ForeColor = Color.FromArgb(120, 200, 120);
                lblNote.Text = _files.Count == 0
                    ? Strings.FileListNone
                    : Strings.Format(_files.Count == 1 ? Strings.FileListCountOne : Strings.FileListCountMany, _files.Count);
                ShowFieldsForSelection();
            }
            catch (RuntimeApiException ex)
            {
                _files = new List<FileDto>();
                gridFiles.Rows.Clear();
                gridFields.Rows.Clear();
                lblNote.ForeColor = Color.FromArgb(235, 130, 120);
                lblNote.Text = ex.Message;
            }
            catch (Exception)
            {
                _files = new List<FileDto>();
                gridFiles.Rows.Clear();
                gridFields.Rows.Clear();
                lblNote.ForeColor = Color.FromArgb(235, 130, 120);
                lblNote.Text = Strings.JobsConnectError;
            }
        }

        private void ShowFieldsForSelection()
        {
            gridFields.Rows.Clear();
            int i = gridFiles.CurrentRow != null ? gridFiles.CurrentRow.Index : -1;
            if (i < 0 || i >= _files.Count) return;

            List<FileFieldDto> fields = _files[i].fields;
            if (fields == null) return;
            foreach (FileFieldDto fld in fields)
            {
                string key = fld.keyPosition > 0 ? fld.keyPosition.ToString() : "";
                gridFields.Rows.Add(fld.name, fld.type, fld.length, fld.decimals, key);
            }
        }

        // ----- helpers de estilo / formato -----

        private static void StyleGrid(DataGridView grid)
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
        }

        private static void AddColumn(DataGridView grid, string header, int width)
        {
            grid.Columns.Add(new DataGridViewTextBoxColumn { HeaderText = header, Width = width });
        }

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
    }
}
