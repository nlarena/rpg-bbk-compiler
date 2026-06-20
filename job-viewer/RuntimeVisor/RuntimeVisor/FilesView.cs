using System;
using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;

namespace RuntimeVisor
{
    /// <summary>
    /// Vista para declarar un archivo de base de datos (physical file), análoga a
    /// CRTPF: nombre + biblioteca + texto y la grilla de campos del formato de
    /// registro. F6 declara (POST /api/files) y el runtime crea la tabla real.
    /// F3 vuelve al menú (lo maneja el HomeForm).
    /// </summary>
    public partial class FilesView : UserControl
    {
        private static readonly Color Info = Color.FromArgb(230, 200, 110);
        private static readonly Color Ok = Color.FromArgb(120, 200, 120);
        private static readonly Color Err = Color.FromArgb(235, 130, 120);

        private readonly RuntimeApiClient _api = new RuntimeApiClient();

        private bool _editMode;
        private string _editLibrary;
        private string _editName;

        public Action<string> Navigate { get; set; }

        public FilesView()
        {
            InitializeComponent();
            LocalizeTexts();
            SetupGrid();
            lblNote.ForeColor = Info;
            lblNote.Text = Strings.FilesHint;
        }

        private void LocalizeTexts()
        {
            lblTitle.Text = Strings.FilesTitle;
            lblFile.Text = Strings.FilesFile;
            lblLibrary.Text = Strings.FilesLibrary;
            lblText.Text = Strings.FilesText;
            lblFkeys.Text = Strings.FilesFkeys;
        }

        /// <summary>Pone la vista en modo edición (CHGPF) sobre un archivo existente.</summary>
        public void LoadForEdit(FileDto file)
        {
            _editMode = true;
            _editLibrary = file.library;
            _editName = file.name;

            lblTitle.Text = Strings.FilesEditTitle;
            lblFkeys.Text = Strings.FilesEditFkeys;

            txtName.Text = file.name;
            txtLibrary.Text = file.library;
            txtText.Text = file.text;
            // la identidad (archivo/biblioteca) no se cambia al modificar
            txtName.ReadOnly = true;
            txtLibrary.ReadOnly = true;

            grid.Rows.Clear();
            if (file.fields != null)
            {
                foreach (FileFieldDto f in file.fields)
                {
                    grid.Rows.Add(f.name, f.type, f.length, f.decimals);
                }
            }
            SetNote(Strings.FilesHint, Info);
        }

        // F6 declara el archivo; F3 vuelve a WRKF (de donde se llega a esta pantalla).
        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (keyData == Keys.F6)
            {
                Submit();
                return true;
            }
            if (keyData == Keys.F3)
            {
                Navigate?.Invoke("FILELIST");
                return true;
            }
            return base.ProcessCmdKey(ref msg, keyData);
        }

        private void SetupGrid()
        {
            grid.AutoGenerateColumns = false;
            grid.AllowUserToAddRows = true;
            grid.AllowUserToResizeRows = false;
            grid.RowHeadersVisible = false;
            grid.MultiSelect = false;
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

            // Un valor de tipo no esperado en el combo no debe tirar la app.
            grid.DataError += (s, e) => { e.ThrowException = false; };

            grid.Columns.Add(new DataGridViewTextBoxColumn { HeaderText = Strings.FilesColField, Width = 160, MaxInputLength = 10 });

            DataGridViewComboBoxColumn typeCol = new DataGridViewComboBoxColumn
            {
                HeaderText = Strings.FilesColType,
                Width = 120,
                FlatStyle = FlatStyle.Flat
            };
            typeCol.Items.AddRange("CHAR", "DECIMAL", "INTEGER", "DATE");
            grid.Columns.Add(typeCol);

            grid.Columns.Add(new DataGridViewTextBoxColumn { HeaderText = Strings.FilesColLength, Width = 90 });
            grid.Columns.Add(new DataGridViewTextBoxColumn { HeaderText = Strings.FilesColDecimals, Width = 100 });
        }

        private async void Submit()
        {
            string name = txtName.Text.Trim();
            string library = txtLibrary.Text.Trim();
            if (name.Length == 0 || library.Length == 0)
            {
                SetNote(Strings.FilesNeedName, Err);
                return;
            }

            List<object> fields = new List<object>();
            foreach (DataGridViewRow row in grid.Rows)
            {
                if (row.IsNewRow) continue;
                string fname = Convert.ToString(row.Cells[0].Value);
                if (string.IsNullOrWhiteSpace(fname)) continue;
                fields.Add(new
                {
                    name = fname.Trim(),
                    type = Convert.ToString(row.Cells[1].Value),
                    length = ParseInt(row.Cells[2].Value),
                    decimals = ParseInt(row.Cells[3].Value)
                });
            }
            if (fields.Count == 0)
            {
                SetNote(Strings.FilesNeedFields, Err);
                return;
            }

            try
            {
                if (_editMode)
                {
                    object request = new { text = txtText.Text.Trim(), fields = fields };
                    FileDto updated = await _api.ModifyFileAsync(_editLibrary, _editName, request);
                    SetNote(Strings.Format(Strings.FilesUpdated, updated.library + "/" + updated.name), Ok);
                }
                else
                {
                    SetNote(Strings.FilesCreating, Info);
                    object request = new { name = name, library = library, text = txtText.Text.Trim(), fields = fields };
                    FileDto created = await _api.DeclareFileAsync(request);
                    SetNote(Strings.Format(Strings.FilesCreated, created.library + "/" + created.name, created.tableName), Ok);
                    // listo para declarar otro
                    txtName.Clear();
                    txtText.Clear();
                    grid.Rows.Clear();
                }
            }
            catch (RuntimeApiException ex)
            {
                SetNote(ex.Message, Err);
            }
            catch (Exception)
            {
                SetNote(Strings.JobsConnectError, Err);
            }
        }

        private void SetNote(string text, Color color)
        {
            lblNote.ForeColor = color;
            lblNote.Text = text;
        }

        private static int ParseInt(object value)
        {
            int n;
            return int.TryParse(Convert.ToString(value), out n) ? n : 0;
        }
    }
}
