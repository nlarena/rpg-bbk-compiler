namespace RuntimeVisor
{
    partial class FilesView
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
            this.lblFile = new System.Windows.Forms.Label();
            this.txtName = new System.Windows.Forms.TextBox();
            this.lblLibrary = new System.Windows.Forms.Label();
            this.txtLibrary = new System.Windows.Forms.TextBox();
            this.lblText = new System.Windows.Forms.Label();
            this.txtText = new System.Windows.Forms.TextBox();
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
            this.lblTitle.Text = "Crear archivo físico                      CRTPF";
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
            // lblFile
            //
            this.lblFile.AutoSize = true;
            this.lblFile.Location = new System.Drawing.Point(24, 82);
            this.lblFile.Name = "lblFile";
            this.lblFile.Text = "Archivo";
            //
            // txtName
            //
            this.txtName.BackColor = System.Drawing.Color.FromArgb(8, 14, 8);
            this.txtName.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.txtName.CharacterCasing = System.Windows.Forms.CharacterCasing.Upper;
            this.txtName.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.txtName.Location = new System.Drawing.Point(150, 79);
            this.txtName.MaxLength = 10;
            this.txtName.Name = "txtName";
            this.txtName.Size = new System.Drawing.Size(130, 23);
            //
            // lblLibrary
            //
            this.lblLibrary.AutoSize = true;
            this.lblLibrary.Location = new System.Drawing.Point(24, 112);
            this.lblLibrary.Name = "lblLibrary";
            this.lblLibrary.Text = "Biblioteca";
            //
            // txtLibrary
            //
            this.txtLibrary.BackColor = System.Drawing.Color.FromArgb(8, 14, 8);
            this.txtLibrary.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.txtLibrary.CharacterCasing = System.Windows.Forms.CharacterCasing.Upper;
            this.txtLibrary.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.txtLibrary.Location = new System.Drawing.Point(150, 109);
            this.txtLibrary.MaxLength = 10;
            this.txtLibrary.Name = "txtLibrary";
            this.txtLibrary.Size = new System.Drawing.Size(130, 23);
            this.txtLibrary.Text = "QGPL";
            //
            // lblText
            //
            this.lblText.AutoSize = true;
            this.lblText.Location = new System.Drawing.Point(24, 142);
            this.lblText.Name = "lblText";
            this.lblText.Text = "Texto";
            //
            // txtText
            //
            this.txtText.BackColor = System.Drawing.Color.FromArgb(8, 14, 8);
            this.txtText.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.txtText.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.txtText.Location = new System.Drawing.Point(150, 139);
            this.txtText.MaxLength = 50;
            this.txtText.Name = "txtText";
            this.txtText.Size = new System.Drawing.Size(360, 23);
            //
            // grid
            //
            this.grid.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom)
                | System.Windows.Forms.AnchorStyles.Left)
                | System.Windows.Forms.AnchorStyles.Right)));
            this.grid.Location = new System.Drawing.Point(24, 178);
            this.grid.Name = "grid";
            this.grid.Size = new System.Drawing.Size(836, 162);
            this.grid.TabIndex = 6;
            //
            // lblFkeys
            //
            this.lblFkeys.Dock = System.Windows.Forms.DockStyle.Bottom;
            this.lblFkeys.ForeColor = System.Drawing.Color.FromArgb(110, 160, 110);
            this.lblFkeys.Location = new System.Drawing.Point(0, 348);
            this.lblFkeys.Name = "lblFkeys";
            this.lblFkeys.Padding = new System.Windows.Forms.Padding(20, 6, 0, 6);
            this.lblFkeys.Size = new System.Drawing.Size(884, 28);
            this.lblFkeys.Text = "F3=Volver    F6=Crear";
            //
            // FilesView
            //
            this.AutoScaleDimensions = new System.Drawing.SizeF(7F, 15F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.BackColor = System.Drawing.Color.FromArgb(12, 20, 12);
            this.Controls.Add(this.grid);
            this.Controls.Add(this.txtText);
            this.Controls.Add(this.lblText);
            this.Controls.Add(this.txtLibrary);
            this.Controls.Add(this.lblLibrary);
            this.Controls.Add(this.txtName);
            this.Controls.Add(this.lblFile);
            this.Controls.Add(this.lblNote);
            this.Controls.Add(this.lblTitle);
            this.Controls.Add(this.lblFkeys);
            this.Font = new System.Drawing.Font("Consolas", 10F);
            this.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.Name = "FilesView";
            this.Size = new System.Drawing.Size(884, 376);
            ((System.ComponentModel.ISupportInitialize)(this.grid)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        #endregion

        private System.Windows.Forms.Label lblTitle;
        private System.Windows.Forms.Label lblNote;
        private System.Windows.Forms.Label lblFile;
        private System.Windows.Forms.TextBox txtName;
        private System.Windows.Forms.Label lblLibrary;
        private System.Windows.Forms.TextBox txtLibrary;
        private System.Windows.Forms.Label lblText;
        private System.Windows.Forms.TextBox txtText;
        private System.Windows.Forms.DataGridView grid;
        private System.Windows.Forms.Label lblFkeys;
    }
}
