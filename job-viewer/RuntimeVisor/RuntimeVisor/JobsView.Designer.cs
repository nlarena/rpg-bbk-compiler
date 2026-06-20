namespace RuntimeVisor
{
    partial class JobsView
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
            this.pnlTop = new System.Windows.Forms.Panel();
            this.lblNote = new System.Windows.Forms.Label();
            this.lblTitle = new System.Windows.Forms.Label();
            this.grid = new System.Windows.Forms.DataGridView();
            this.lblFkeys = new System.Windows.Forms.Label();
            this.pnlTop.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)(this.grid)).BeginInit();
            this.SuspendLayout();
            //
            // pnlTop
            //
            this.pnlTop.Controls.Add(this.lblNote);
            this.pnlTop.Controls.Add(this.lblTitle);
            this.pnlTop.Dock = System.Windows.Forms.DockStyle.Top;
            this.pnlTop.Location = new System.Drawing.Point(0, 0);
            this.pnlTop.Name = "pnlTop";
            this.pnlTop.Size = new System.Drawing.Size(884, 72);
            //
            // lblTitle
            //
            this.lblTitle.AutoSize = true;
            this.lblTitle.Font = new System.Drawing.Font("Consolas", 13F, System.Drawing.FontStyle.Bold);
            this.lblTitle.ForeColor = System.Drawing.Color.FromArgb(120, 230, 120);
            this.lblTitle.Location = new System.Drawing.Point(20, 14);
            this.lblTitle.Name = "lblTitle";
            this.lblTitle.Text = "Trabajos activos                          WRKACTJOB";
            //
            // lblNote
            //
            this.lblNote.AutoSize = true;
            this.lblNote.Font = new System.Drawing.Font("Consolas", 9F);
            this.lblNote.ForeColor = System.Drawing.Color.FromArgb(230, 200, 110);
            this.lblNote.Location = new System.Drawing.Point(22, 46);
            this.lblNote.Name = "lblNote";
            this.lblNote.Text = "Consultando trabajos…";
            //
            // grid
            //
            this.grid.Dock = System.Windows.Forms.DockStyle.Fill;
            this.grid.Location = new System.Drawing.Point(0, 72);
            this.grid.Name = "grid";
            this.grid.Size = new System.Drawing.Size(884, 276);
            this.grid.TabIndex = 0;
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
            // JobsView
            //
            this.AutoScaleDimensions = new System.Drawing.SizeF(7F, 15F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.BackColor = System.Drawing.Color.FromArgb(12, 20, 12);
            this.Controls.Add(this.grid);
            this.Controls.Add(this.pnlTop);
            this.Controls.Add(this.lblFkeys);
            this.Font = new System.Drawing.Font("Consolas", 10F);
            this.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.Name = "JobsView";
            this.Size = new System.Drawing.Size(884, 376);
            this.pnlTop.ResumeLayout(false);
            this.pnlTop.PerformLayout();
            ((System.ComponentModel.ISupportInitialize)(this.grid)).EndInit();
            this.ResumeLayout(false);
        }

        #endregion

        private System.Windows.Forms.Panel pnlTop;
        private System.Windows.Forms.Label lblTitle;
        private System.Windows.Forms.Label lblNote;
        private System.Windows.Forms.DataGridView grid;
        private System.Windows.Forms.Label lblFkeys;
    }
}
