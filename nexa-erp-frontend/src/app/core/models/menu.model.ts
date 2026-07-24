export interface AppMenuItem {
  label: string;
  icon?: string;
  route?: string;
  permission?: string;
  children?: AppMenuItem[];
}