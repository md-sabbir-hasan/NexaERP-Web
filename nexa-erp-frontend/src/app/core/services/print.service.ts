import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class PrintService {
  printElement(
    elementId: string,
    title: string,
    orientation: 'portrait' | 'landscape' = 'portrait',
  ): void {
    const element = document.getElementById(elementId);

    if (!element) {
      console.error(`Print element not found: ${elementId}`);
      return;
    }

    const printWindow = window.open('', '_blank', 'width=1200,height=850,noopener,noreferrer');

    if (!printWindow) {
      console.error('Print window could not be opened');
      return;
    }

    const styles = this.collectStyles();

    printWindow.document.open();

    printWindow.document.write(`
      <!DOCTYPE html>
      <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta
            name="viewport"
            content="width=device-width, initial-scale=1.0"
          />

          <title>${this.escapeHtml(title)}</title>

          ${styles}

          <style>
            @page {
              size: A4 ${orientation};
              margin: 14mm;
            }

            * {
              box-sizing: border-box;
            }

            html,
            body {
              margin: 0;
              padding: 0;
              background: #ffffff;
              color: #0f172a;
              font-family:
                Arial,
                Helvetica,
                sans-serif;
              -webkit-print-color-adjust: exact;
              print-color-adjust: exact;
            }

            body {
              width: 100%;
            }

            .print-document {
              width: 100%;
              max-width: none;
              margin: 0;
              padding: 0;
            }

            .report-hero,
            .filter-card,
            .report-actions,
            .back-btn,
            .clear-btn,
            .generate-btn,
            .print-btn,
            .pdf-btn {
              display: none !important;
            }

            .table-wrap {
              overflow: visible !important;
            }

            table {
              width: 100% !important;
              min-width: 0 !important;
              border-collapse: collapse !important;
              page-break-inside: auto;
            }

            thead {
              display: table-header-group;
            }

            tfoot {
              display: table-footer-group;
            }

            tr {
              page-break-inside: avoid;
              page-break-after: auto;
            }

            .statement-grid,
            .summary-grid,
            .content-grid {
              page-break-inside: avoid;
            }

            button,
            a {
              box-shadow: none !important;
            }
          </style>
        </head>

        <body>
          <main class="print-document">
            ${element.outerHTML}
          </main>
        </body>
      </html>
    `);

    printWindow.document.close();

    printWindow.onload = () => {
      window.setTimeout(() => {
        printWindow.focus();
        printWindow.print();

        printWindow.onafterprint = () => {
          printWindow.close();
        };
      }, 300);
    };
  }

  private collectStyles(): string {
    const styleNodes = Array.from(
      document.querySelectorAll<HTMLStyleElement | HTMLLinkElement>(
        'style, link[rel="stylesheet"]',
      ),
    );

    return styleNodes
      .map((node) => {
        if (node.tagName.toLowerCase() === 'style') {
          return `<style>${node.textContent ?? ''}</style>`;
        }

        const href = (node as HTMLLinkElement).href;

        return href ? `<link rel="stylesheet" href="${href}" />` : '';
      })
      .join('\n');
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }
}
