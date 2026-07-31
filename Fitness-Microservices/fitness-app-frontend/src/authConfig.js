const authConfig = {
    clientId: 'fitness-oauth2-realm',
    authorizationEndpoint: 'http://localhost:8181/realms/master/protocol/openid-connect/auth',
    tokenEndpoint: 'http://localhost:8181/realms/master/protocol/openid-connect/token',
    redirectUri: 'http://localhost:5173/',
    scope: 'openid profile email offline_access',
    onRefreshTokenExpire: (event) => event.logIn(),
  }

export default authConfig;