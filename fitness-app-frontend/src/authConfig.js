export const authConfig = {
    clientId: 'oauth2-pkce-client',
    authorizationEndpoint: `${import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8181'}/realms/fitness-oauth2/protocol/openid-connect/auth`,
    tokenEndpoint: `${import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8181'}/realms/fitness-oauth2/protocol/openid-connect/token`,
    redirectUri: import.meta.env.VITE_REDIRECT_URI || 'http://localhost:5173/',
    scope: 'openid profile email offline_access',
    onRefreshTokenExpire: (event) => event.logIn(),
  }