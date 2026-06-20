using System;
using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;

namespace RuntimeVisor
{
    /// <summary>
    /// Consola SQL interactiva (análoga a STRSQL): se escribe una sentencia y F6 la
    /// ejecuta directo contra la base. Un SELECT llena la grilla con sus columnas;
    /// un INSERT/UPDATE/DDL muestra las filas afectadas. F3 vuelve (lo maneja el HomeForm).
    /// </summary>
    public partial class SqlView : UserControl
    {
        private static readonly Color Info = Color.FromArgb(230, 200, 110);
        private static readonly Color Ok = Color.FromArgb(120, 200, 120);
        private static readonly Color Err = Color.FromArgb(235, 130, 120);

        private readonly RuntimeApiClient _api = new RuntimeApiClient();

        public Action<string> Navigate { get; set; }

        public SqlView()
        {
            InitializeComponent();
            LocalizeTexts();
            SetupGrid();
            // sentencia de ejemplo, para ejecutar de una con F6
            txtSql.Text = "SELECT job_number, name, status FROM job";
            SetNote(Strings.SqlHint, Info);
        }

        private void LocalizeTexts()
        {
            lblTitle.Text = Strings.SqlTitle;
            lblPrompt.Text = Strings.SqlPrompt;
            lblFkeys.Text = Strings.SqlFkeys;
        }

        // F6 ejecuta; F3 lo maneja el HomeForm (vuelve al menú).
        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (keyData == Keys.F6)
            {
                Run();
                return true;
            }
            return base.ProcessCmdKey(ref msg, keyData);
        }

        private async void Run()
        {
            string sql = txtSql.Text.Trim();
            if (sql.Length == 0)
            {
                SetNote(Strings.SqlHint, Info);
                return;
            }

            SetNote(Strings.SqlRunning, Info);
            try
            {
                SqlResultDto result = await _api.RunSqlAsync(sql);

                grid.Columns.Clear();
                grid.Rows.Clear();

                if (result.kind == "UPDATE")
                {
                    SetNote(Strings.Format(Strings.SqlUpdated, result.updateCount ?? 0), Ok);
                    return;
                }

                if (result.columns != null)
                {
                    foreach (string col in result.columns)
                    {
                        grid.Columns.Add(new DataGridViewTextBoxColumn
                        {
                            HeaderText = col,
                            SortMode = DataGridViewColumnSortMode.NotSortable
                        });
                    }
                }

                int count = 0;
                if (result.rows != null)
                {
                    foreach (List<string> row in result.rows)
                    {
                        object[] cells = new object[row.Count];
                        for (int i = 0; i < row.Count; i++) cells[i] = row[i] ?? "(null)";
                        grid.Rows.Add(cells);
                        count++;
                    }
                }

                if (count == 0) SetNote(Strings.SqlNoRows, Ok);
                else if (result.truncated) SetNote(Strings.Format(Strings.SqlTruncated, count), Info);
                else SetNote(Strings.Format(count == 1 ? Strings.SqlRowsOne : Strings.SqlRowsMany, count), Ok);
            }
            catch (RuntimeApiException ex)
            {
                grid.Columns.Clear();
                grid.Rows.Clear();
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

        private void SetupGrid()
        {
            grid.AutoGenerateColumns = false;
            grid.ReadOnly = true;
            grid.AllowUserToAddRows = false;
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
        }
    }
}
