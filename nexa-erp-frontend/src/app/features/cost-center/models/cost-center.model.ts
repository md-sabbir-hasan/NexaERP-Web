export interface CostCenter {
  id: number;
  code: string;
  name: string;
  description: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CostCenterLookup {
  id: number;
  code: string;
  name: string;
}

export interface CostCenterRequest {
  code: string;
  name: string;
  description?: string;
}
