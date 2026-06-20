namespace RuntimeVisor
{
    partial class MainMenuView
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
            this.lblHeader = new System.Windows.Forms.Label();
            this.lblOption1 = new System.Windows.Forms.Label();
            this.lblOption2 = new System.Windows.Forms.Label();
            this.lblOption3 = new System.Windows.Forms.Label();
            this.lblPrompt = new System.Windows.Forms.Label();
            this.txtCommand = new System.Windows.Forms.TextBox();
            this.lblStatus = new System.Windows.Forms.Label();
            this.lblFkeys = new System.Windows.Forms.Label();
            this.SuspendLayout();
            //
            // lblHeader
            //
            this.lblHeader.AutoSize = true;
            this.lblHeader.Location = new System.Drawing.Point(24, 22);
            this.lblHeader.Name = "lblHeader";
            this.lblHeader.Text = "Seleccione una opción:";
            //
            // lblOption1
            //
            this.lblOption1.AutoSize = true;
            this.lblOption1.Cursor = System.Windows.Forms.Cursors.Hand;
            this.lblOption1.Location = new System.Drawing.Point(44, 58);
            this.lblOption1.Name = "lblOption1";
            this.lblOption1.Text = "1.  Trabajos activos                  WRKACTJOB";
            this.lblOption1.Click += new System.EventHandler(this.lblOption1_Click);
            //
            // lblOption2
            //
            this.lblOption2.AutoSize = true;
            this.lblOption2.Cursor = System.Windows.Forms.Cursors.Hand;
            this.lblOption2.Location = new System.Drawing.Point(44, 84);
            this.lblOption2.Name = "lblOption2";
            this.lblOption2.Text = "2.  Trabajar con archivos             WRKF";
            this.lblOption2.Click += new System.EventHandler(this.lblOption2_Click);
            //
            // lblOption3
            //
            this.lblOption3.AutoSize = true;
            this.lblOption3.Cursor = System.Windows.Forms.Cursors.Hand;
            this.lblOption3.Location = new System.Drawing.Point(44, 110);
            this.lblOption3.Name = "lblOption3";
            this.lblOption3.Text = "3.  SQL interactivo                   STRSQL";
            this.lblOption3.Click += new System.EventHandler(this.lblOption3_Click);
            //
            // lblPrompt
            //
            this.lblPrompt.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.lblPrompt.AutoSize = true;
            this.lblPrompt.Location = new System.Drawing.Point(24, 291);
            this.lblPrompt.Name = "lblPrompt";
            this.lblPrompt.Text = "Opción/Comando ===>";
            //
            // txtCommand
            //
            this.txtCommand.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.txtCommand.BackColor = System.Drawing.Color.FromArgb(8, 14, 8);
            this.txtCommand.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.txtCommand.CharacterCasing = System.Windows.Forms.CharacterCasing.Upper;
            this.txtCommand.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.txtCommand.Location = new System.Drawing.Point(212, 288);
            this.txtCommand.Name = "txtCommand";
            this.txtCommand.Size = new System.Drawing.Size(420, 23);
            this.txtCommand.KeyDown += new System.Windows.Forms.KeyEventHandler(this.txtCommand_KeyDown);
            //
            // lblStatus
            //
            this.lblStatus.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.lblStatus.AutoSize = true;
            this.lblStatus.ForeColor = System.Drawing.Color.FromArgb(230, 200, 110);
            this.lblStatus.Location = new System.Drawing.Point(24, 320);
            this.lblStatus.Name = "lblStatus";
            this.lblStatus.Text = "";
            //
            // lblFkeys
            //
            this.lblFkeys.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.lblFkeys.AutoSize = true;
            this.lblFkeys.ForeColor = System.Drawing.Color.FromArgb(110, 160, 110);
            this.lblFkeys.Location = new System.Drawing.Point(24, 345);
            this.lblFkeys.Name = "lblFkeys";
            this.lblFkeys.Text = "F3=Salir";
            //
            // MainMenuView
            //
            this.AutoScaleDimensions = new System.Drawing.SizeF(7F, 15F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.BackColor = System.Drawing.Color.FromArgb(12, 20, 12);
            this.Controls.Add(this.lblFkeys);
            this.Controls.Add(this.lblStatus);
            this.Controls.Add(this.txtCommand);
            this.Controls.Add(this.lblPrompt);
            this.Controls.Add(this.lblOption3);
            this.Controls.Add(this.lblOption2);
            this.Controls.Add(this.lblOption1);
            this.Controls.Add(this.lblHeader);
            this.Font = new System.Drawing.Font("Consolas", 10F);
            this.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.Name = "MainMenuView";
            this.Size = new System.Drawing.Size(884, 376);
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        #endregion

        private System.Windows.Forms.Label lblHeader;
        private System.Windows.Forms.Label lblOption1;
        private System.Windows.Forms.Label lblOption2;
        private System.Windows.Forms.Label lblOption3;
        private System.Windows.Forms.Label lblPrompt;
        private System.Windows.Forms.TextBox txtCommand;
        private System.Windows.Forms.Label lblStatus;
        private System.Windows.Forms.Label lblFkeys;
    }
}
