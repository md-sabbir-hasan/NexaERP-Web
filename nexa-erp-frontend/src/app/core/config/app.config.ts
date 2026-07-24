import { environment } from "../../../environments/environment";



export const APP_CONFIG = {
  appName: environment.appName,
  version: environment.version,
  apiUrl: environment.apiUrl,

  pagination: {
    defaultPage: 0,
    defaultSize: 10,
    pageSizeOptions: [5, 10, 20, 50, 100],
  },

  storageKeys: {
    accessToken: 'nexa_access_token',
    refreshToken: 'nexa_refresh_token',
    currentUser: 'nexa_current_user',
  },

  formats: {
    date: 'yyyy-MM-dd',
    dateTime: 'yyyy-MM-dd HH:mm',
    currency: 'BDT',
  },
} as const;
