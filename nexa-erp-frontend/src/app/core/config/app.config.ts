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

  formats: {
    date: 'yyyy-MM-dd',
    dateTime: 'yyyy-MM-dd HH:mm',
    currency: 'BDT',
  },
} as const;
