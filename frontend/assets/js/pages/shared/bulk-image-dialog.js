import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { errorMessage } from '../../core/errors.js';
import { openModal } from '../../components/overlay.js';
import { setLoading } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { medicineService } from '../../services/catalog-service.js';

const statusText = {
  SAVED: ['success', 'Đã lưu'],
  SKIPPED_EXISTING: ['neutral', 'Bỏ qua'],
  NO_MATCH: ['warning', 'Không khớp'],
  INVALID: ['danger', 'Lỗi ảnh'],
};

const resultView = (result) => html`<div class="stack">
  <p><strong>${result.saved}</strong> ảnh đã lưu · ${result.skipped} bỏ qua · ${result.noMatch} không khớp thuốc · ${result.invalid} lỗi ảnh</p>
  <div class="table-wrap"><table class="table"><thead><tr><th>File</th><th>Thuốc</th><th>Kết quả</th></tr></thead>
    <tbody>${result.items.map(
      (
        item,
      ) => html`<tr><td>${item.fileName}</td><td>${item.medicineName ? `${item.medicineCode} · ${item.medicineName}` : '—'}</td>
      <td><span class="badge badge--${statusText[item.status][0]}">${statusText[item.status][1]}</span><br /><small class="text-secondary">${item.message}</small></td></tr>`,
    )}</tbody></table></div>
</div>`;

export const openBulkImageDialog = () =>
  new Promise((resolve) => {
    const modal = openModal({
      title: 'Tải ảnh thuốc hàng loạt',
      size: 'lg',
      content: html`<div class="stack" data-body>
        <div class="alert alert--info">${icon('info')}<span>Đặt tên mỗi ảnh theo <strong>mã thuốc</strong> (ví dụ <code>T0002.jpg</code>), mã vạch hoặc số đăng ký. Có thể chọn nhiều ảnh hoặc một file <code>.zip</code>. Ảnh JPG, PNG, WebP tối đa 2 MB mỗi ảnh, tối đa 300 ảnh mỗi lần. Ảnh lớn hơn 600px sẽ được thu nhỏ.</span></div>
        <div class="form-field"><label class="form-label" for="bulk-files">Chọn ảnh hoặc file zip</label>
          <input class="form-control" id="bulk-files" type="file" multiple accept="image/jpeg,image/png,image/webp,.zip" data-files /></div>
        <label class="check"><input type="checkbox" data-overwrite /> Ghi đè ảnh đã có</label>
        <div data-result></div>
      </div>`,
      footer: html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button><button type="button" class="btn btn--primary" data-upload>${icon('upload', 'icon--sm')} Tải lên</button>`,
    });
    const filesInput = modal.body.querySelector('[data-files]');
    const overwrite = modal.body.querySelector('[data-overwrite]');
    const output = modal.body.querySelector('[data-result]');
    const button = modal.footer.querySelector('[data-upload]');
    let saved = 0;
    button.addEventListener('click', async () => {
      const files = [...filesInput.files];
      if (!files.length) {
        toast.warning('Chưa chọn ảnh', 'Hãy chọn ít nhất một ảnh hoặc file zip.');
        return;
      }
      setLoading(button, true);
      try {
        const result = await medicineService.uploadImages(files, overwrite.checked);
        saved += result.saved;
        output.innerHTML = resultView(result).value;
        toast.success('Đã xử lý ảnh', `${result.saved} ảnh được lưu.`);
      } catch (error) {
        toast.error('Không thể tải ảnh', errorMessage(error));
      } finally {
        setLoading(button, false);
      }
    });
    modal.closed.then(() => resolve(saved > 0));
  });
