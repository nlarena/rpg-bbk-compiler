namespace RuntimeVisor
{
    partial class HomeForm
    {
        private System.ComponentModel.IContainer components = null;

        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        private void InitializeComponent()
        {
            this.components = new System.ComponentModel.Container();
            this.pnlHeader = new System.Windows.Forms.Panel();
            this.lblHeaderInfo = new System.Windows.Forms.Label();
            this.lblTitle = new System.Windows.Forms.Label();
            this.pnlBody = new System.Windows.Forms.Panel();
            this.clockTimer = new System.Windows.Forms.Timer(this.components);
            this.pnlHeader.SuspendLayout();
            this.SuspendLayout();
            //
            // pnlHeader
            //
            this.pnlHeader.BackColor = System.Drawing.Color.FromArgb(12, 20, 12);
            this.pnlHeader.Controls.Add(this.lblHeaderInfo);
            this.pnlHeader.Controls.Add(this.lblTitle);
            this.pnlHeader.Dock = System.Windows.Forms.DockStyle.Top;
            this.pnlHeader.Location = new System.Drawing.Point(0, 0);
            this.pnlHeader.Name = "pnlHeader";
            this.pnlHeader.Padding = new System.Windows.Forms.Padding(20, 14, 20, 14);
            this.pnlHeader.Size = new System.Drawing.Size(884, 205);
            //
            // lblHeaderInfo
            //
            this.lblHeaderInfo.AutoSize = true;
            this.lblHeaderInfo.Font = new System.Drawing.Font("Consolas", 10F);
            this.lblHeaderInfo.ForeColor = System.Drawing.Color.FromArgb(150, 220, 150);
            this.lblHeaderInfo.Location = new System.Drawing.Point(22, 58);
            this.lblHeaderInfo.Name = "lblHeaderInfo";
            this.lblHeaderInfo.Size = new System.Drawing.Size(0, 15);
            this.lblHeaderInfo.TabIndex = 1;
            //
            // lblTitle
            //
            this.lblTitle.AutoSize = true;
            this.lblTitle.Font = new System.Drawing.Font("Consolas", 17F, System.Drawing.FontStyle.Bold);
            this.lblTitle.ForeColor = System.Drawing.Color.FromArgb(120, 230, 120);
            this.lblTitle.Location = new System.Drawing.Point(20, 14);
            this.lblTitle.Name = "lblTitle";
            this.lblTitle.Size = new System.Drawing.Size(283, 27);
            this.lblTitle.TabIndex = 0;
            this.lblTitle.Text = "BoxBreaker — Runtime";
            //
            // pnlBody
            //
            this.pnlBody.BackColor = System.Drawing.Color.FromArgb(12, 20, 12);
            this.pnlBody.Dock = System.Windows.Forms.DockStyle.Fill;
            this.pnlBody.Location = new System.Drawing.Point(0, 205);
            this.pnlBody.Name = "pnlBody";
            this.pnlBody.Size = new System.Drawing.Size(884, 356);
            this.pnlBody.TabIndex = 1;
            //
            // clockTimer
            //
            this.clockTimer.Interval = 1000;
            this.clockTimer.Tick += new System.EventHandler(this.clockTimer_Tick);
            //
            // HomeForm
            //
            this.AutoScaleDimensions = new System.Drawing.SizeF(7F, 15F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(884, 561);
            this.Controls.Add(this.pnlBody);
            this.Controls.Add(this.pnlHeader);
            this.MinimumSize = new System.Drawing.Size(700, 420);
            this.Name = "HomeForm";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
            this.Text = "BoxBreaker — Runtime";
            this.pnlHeader.ResumeLayout(false);
            this.pnlHeader.PerformLayout();
            this.ResumeLayout(false);
        }

        #endregion

        private System.Windows.Forms.Panel pnlHeader;
        private System.Windows.Forms.Label lblTitle;
        private System.Windows.Forms.Label lblHeaderInfo;
        private System.Windows.Forms.Panel pnlBody;
        private System.Windows.Forms.Timer clockTimer;
    }
}
