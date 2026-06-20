namespace RuntimeVisor
{
    partial class FileListView
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
            this.gridFiles = new System.Windows.Forms.DataGridView();
            this.lblFmt = new System.Windows.Forms.Label();
            this.gridFields = new System.Windows.Forms.DataGridView();
            this.lblFkeys = new System.Windows.Forms.Label();
            ((System.ComponentModel.ISupportInitialize)(this.gridFiles)).BeginInit();
            ((System.ComponentModel.ISupportInitialize)(this.gridFields)).BeginInit();
            this.SuspendLayout();
            //
            // lblTitle
            //
            this.lblTitle.AutoSize = true;
            this.lblTitle.Font = new System.Drawing.Font("Consolas", 13F, System.Drawing.FontStyle.Bold);
            this.lblTitle.ForeColor = System.Drawing.Color.FromArgb(120, 230, 120);
            this.lblTitle.Location = new System.Drawing.Point(20, 14);
            this.lblTitle.Name = "lblTitle";
            this.lblTitle.Text = "Trabajar con archivos                     WRKF";
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
            // gridFiles
            //
            this.gridFiles.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom)
                | System.Windows.Forms.AnchorStyles.Left)
                | System.Windows.Forms.AnchorStyles.Right)));
            this.gridFiles.Location = new System.Drawing.Point(12, 72);
            this.gridFiles.Name = "gridFiles";
            this.gridFiles.Size = new System.Drawing.Size(860, 150);
            this.gridFiles.TabIndex = 0;
            //
            // lblFmt
            //
            this.lblFmt.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.lblFmt.AutoSize = true;
            this.lblFmt.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.lblFmt.Location = new System.Drawing.Point(14, 230);
            this.lblFmt.Name = "lblFmt";
            this.lblFmt.Text = "Formato de registro:";
            //
            // gridFields
            //
            this.gridFields.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)
                | System.Windows.Forms.AnchorStyles.Right))));
            this.gridFields.Location = new System.Drawing.Point(12, 252);
            this.gridFields.Name = "gridFields";
            this.gridFields.Size = new System.Drawing.Size(860, 82);
            this.gridFields.TabIndex = 1;
            //
            // lblFkeys
            //
            this.lblFkeys.Dock = System.Windows.Forms.DockStyle.Bottom;
            this.lblFkeys.ForeColor = System.Drawing.Color.FromArgb(110, 160, 110);
            this.lblFkeys.Location = new System.Drawing.Point(0, 348);
            this.lblFkeys.Name = "lblFkeys";
            this.lblFkeys.Padding = new System.Windows.Forms.Padding(20, 6, 0, 6);
            this.lblFkeys.Size = new System.Drawing.Size(884, 28);
            this.lblFkeys.Text = "F3=Volver    F5=Refrescar";
            //
            // FileListView
            //
            this.AutoScaleDimensions = new System.Drawing.SizeF(7F, 15F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.BackColor = System.Drawing.Color.FromArgb(12, 20, 12);
            this.Controls.Add(this.gridFields);
            this.Controls.Add(this.lblFmt);
            this.Controls.Add(this.gridFiles);
            this.Controls.Add(this.lblNote);
            this.Controls.Add(this.lblTitle);
            this.Controls.Add(this.lblFkeys);
            this.Font = new System.Drawing.Font("Consolas", 10F);
            this.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.Name = "FileListView";
            this.Size = new System.Drawing.Size(884, 376);
            ((System.ComponentModel.ISupportInitialize)(this.gridFiles)).EndInit();
            ((System.ComponentModel.ISupportInitialize)(this.gridFields)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        #endregion

        private System.Windows.Forms.Label lblTitle;
        private System.Windows.Forms.Label lblNote;
        private System.Windows.Forms.DataGridView gridFiles;
        private System.Windows.Forms.Label lblFmt;
        private System.Windows.Forms.DataGridView gridFields;
        private System.Windows.Forms.Label lblFkeys;
    }
}
