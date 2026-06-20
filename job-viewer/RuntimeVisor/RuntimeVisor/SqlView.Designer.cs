namespace RuntimeVisor
{
    partial class SqlView
    {
        private System.ComponentModel.IContainer components = null;

        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null)) { components.Dispose(); }
            base.Dispose(disposing);
        }

        #region Component Designer generated code

        private void InitializeComponent()
        {
            this.lblTitle = new System.Windows.Forms.Label();
            this.lblNote = new System.Windows.Forms.Label();
            this.lblPrompt = new System.Windows.Forms.Label();
            this.txtSql = new System.Windows.Forms.TextBox();
            this.grid = new System.Windows.Forms.DataGridView();
            this.lblFkeys = new System.Windows.Forms.Label();
            ((System.ComponentModel.ISupportInitialize)(this.grid)).BeginInit();
            this.SuspendLayout();
            //
            // lblTitle
            //
            this.lblTitle.AutoSize = true;
            this.lblTitle.Font = new System.Drawing.Font("Consolas", 13F, System.Drawing.FontStyle.Bold);
            this.lblTitle.ForeColor = System.Drawing.Color.FromArgb(120, 230, 120);
            this.lblTitle.Location = new System.Drawing.Point(20, 14);
            this.lblTitle.Name = "lblTitle";
            this.lblTitle.Text = "SQL interactivo                           STRSQL";
            //
            // lblNote
            //
            this.lblNote.AutoSize = true;
            this.lblNote.Font = new System.Drawing.Font("Consolas", 9F);
            this.lblNote.ForeColor = System.Drawing.Color.FromArgb(230, 200, 110);
            this.lblNote.Location = new System.Drawing.Point(22, 46);
            this.lblNote.Name = "lblNote";
            this.lblNote.Text = "";
            //
            // lblPrompt
            //
            this.lblPrompt.AutoSize = true;
            this.lblPrompt.Location = new System.Drawing.Point(24, 74);
            this.lblPrompt.Name = "lblPrompt";
            this.lblPrompt.Text = "Sentencia SQL:";
            //
            // txtSql
            //
            this.txtSql.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left)
                | System.Windows.Forms.AnchorStyles.Right)));
            this.txtSql.BackColor = System.Drawing.Color.FromArgb(8, 14, 8);
            this.txtSql.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.txtSql.Font = new System.Drawing.Font("Consolas", 10F);
            this.txtSql.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.txtSql.Location = new System.Drawing.Point(24, 96);
            this.txtSql.Multiline = true;
            this.txtSql.Name = "txtSql";
            this.txtSql.ScrollBars = System.Windows.Forms.ScrollBars.Vertical;
            this.txtSql.Size = new System.Drawing.Size(836, 84);
            this.txtSql.TabIndex = 0;
            //
            // grid
            //
            this.grid.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom)
                | System.Windows.Forms.AnchorStyles.Left)
                | System.Windows.Forms.AnchorStyles.Right)));
            this.grid.Location = new System.Drawing.Point(24, 192);
            this.grid.Name = "grid";
            this.grid.Size = new System.Drawing.Size(836, 148);
            this.grid.TabIndex = 1;
            //
            // lblFkeys
            //
            this.lblFkeys.Dock = System.Windows.Forms.DockStyle.Bottom;
            this.lblFkeys.ForeColor = System.Drawing.Color.FromArgb(110, 160, 110);
            this.lblFkeys.Location = new System.Drawing.Point(0, 348);
            this.lblFkeys.Name = "lblFkeys";
            this.lblFkeys.Padding = new System.Windows.Forms.Padding(20, 6, 0, 6);
            this.lblFkeys.Size = new System.Drawing.Size(884, 28);
            this.lblFkeys.Text = "F3=Volver    F6=Ejecutar";
            //
            // SqlView
            //
            this.AutoScaleDimensions = new System.Drawing.SizeF(7F, 15F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.BackColor = System.Drawing.Color.FromArgb(12, 20, 12);
            this.Controls.Add(this.grid);
            this.Controls.Add(this.txtSql);
            this.Controls.Add(this.lblPrompt);
            this.Controls.Add(this.lblNote);
            this.Controls.Add(this.lblTitle);
            this.Controls.Add(this.lblFkeys);
            this.Font = new System.Drawing.Font("Consolas", 10F);
            this.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.Name = "SqlView";
            this.Size = new System.Drawing.Size(884, 376);
            ((System.ComponentModel.ISupportInitialize)(this.grid)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        #endregion

        private System.Windows.Forms.Label lblTitle;
        private System.Windows.Forms.Label lblNote;
        private System.Windows.Forms.Label lblPrompt;
        private System.Windows.Forms.TextBox txtSql;
        private System.Windows.Forms.DataGridView grid;
        private System.Windows.Forms.Label lblFkeys;
    }
}
