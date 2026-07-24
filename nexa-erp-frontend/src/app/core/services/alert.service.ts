import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root',
})
export class AlertService {
  success(message: string): void {
    void Swal.fire({
      icon: 'success',
      title: 'Success',
      text: message,
    });
  }

  error(message: string): void {
    void Swal.fire({
      icon: 'error',
      title: 'Error',
      text: message,
    });
  }

  warning(message: string): void {
    void Swal.fire({
      icon: 'warning',
      title: 'Warning',
      text: message,
    });
  }

  async confirm(message: string): Promise<boolean> {
    const result = await Swal.fire({
      icon: 'question',
      title: 'Confirmation',
      text: message,
      showCancelButton: true,
      confirmButtonText: 'Yes',
      cancelButtonText: 'No',
    });

    return result.isConfirmed;
  }
}
