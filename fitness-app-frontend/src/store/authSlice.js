import { createSlice } from '@reduxjs/toolkit'

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    userId: localStorage.getItem('userId') || null
  },
  reducers: {
    setCredentials: (state, action) => {
        state.user = action.payload.user;
        state.token = action.payload.token;
        state.userId = action.payload.user.sub;

        localStorage.setItem('token', action.payload.token);
        localStorage.setItem('user', JSON.stringify(action.payload.user));
        localStorage.setItem('userId', action.payload.user.sub); // This is KeycloakId 
    },
    logOut: (state) => {
        state.user = null;
        state.token = null;
        state.userId = null;

        localStorage.removeItem('token');
        localStorage.removeItem('user');
        localStorage.removeItem('userId');
    }
  },
});

export const { setCredentials, logOut } = authSlice.actions;
export default authSlice.reducer;