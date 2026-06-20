using System;
using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;

namespace RuntimeVisor
{
    /// <summary>Pantalla de sign-on contra el runtime (estilo IBM i): usuario + contraseña → JWT.</summary>
    public partial class Form1 : Form
    {
        private readonly RuntimeApiClient _api = new RuntimeApiClient();

        public Form1()
        {
            InitializeComponent();
            LocalizeTexts();

            // TODO(dev): credenciales precargadas para no tipear en cada login.
            // QUITAR antes de cualquier uso real (riesgo de seguridad). Ver TODO.md.
            txtUser.Text = "QSECOFR";
            txtPassword.Text = "qsecofr";
        }

        private void LocalizeTexts()
        {
            this.Text = Strings.LoginWindowTitle;
            lblUser.Text = Strings.LoginUser;
            lblPassword.Text = Strings.LoginPassword;
            btnLogin.Text = Strings.LoginButton;
        }

        private async void btnLogin_Click(object sender, EventArgs e)
        {
            string user = txtUser.Text.Trim();
            string password = txtPassword.Text;

            if (user.Length == 0 || password.Length == 0)
            {
                SetStatus(Strings.LoginEnterCredentials, StatusKind.Error);
                return;
            }

            btnLogin.Enabled = false;
            SetStatus(Strings.LoginConnecting, StatusKind.Info);
            try
            {
                LoginResponse result = await _api.LoginAsync(user, password);
                RuntimeSession.SignIn(result);

                // login OK -> abrir el Home y ocultar la pantalla de login.
                // Al cerrarse el Home, se cierra el login (oculto) y termina la app.
                HomeForm home = new HomeForm();
                home.FormClosed += (s, args) => this.Close();
                this.Hide();
                home.Show();
            }
            catch (RuntimeApiException ex)
            {
                SetStatus(ex.Message, StatusKind.Error);
                btnLogin.Enabled = true;
            }
            catch (Exception ex)
            {
                SetStatus(Strings.Format(Strings.LoginConnectError, ex.Message), StatusKind.Error);
                btnLogin.Enabled = true;
            }
        }

        private enum StatusKind { Info, Success, Error }

        private void SetStatus(string text, StatusKind kind)
        {
            switch (kind)
            {
                case StatusKind.Success: lblStatus.ForeColor = Color.SeaGreen; break;
                case StatusKind.Error: lblStatus.ForeColor = Color.Firebrick; break;
                default: lblStatus.ForeColor = Color.DimGray; break;
            }
            lblStatus.Text = text;
        }
    }
}
