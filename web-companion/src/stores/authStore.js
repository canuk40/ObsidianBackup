import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
  persist(
    (set, get) => ({
      token: null,
      deviceId: null,
      deviceName: null,
      isAuthenticated: false,
      
      setAuth: (token, deviceId, deviceName) => {
        set({
          token,
          deviceId,
          deviceName,
          isAuthenticated: true
        });
      },
      
      clearAuth: () => {
        set({
          token: null,
          deviceId: null,
          deviceName: null,
          isAuthenticated: false
        });
      },
      
      initialize: () => {
        const state = get();
        if (state.token) {
          set({ isAuthenticated: true });
        }
      }
    }),
    {
      name: 'obsidianbackup-auth'
    }
  )
);
