import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { errorMessage } from '../../core/errors.js';
import { startPage } from '../../components/page.js';
import { card, pageHeading } from '../../components/blocks.js';
import {
  clearErrors,
  readValues,
  renderFields,
  setLoading,
  showErrors,
  validateFields,
} from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { preferences } from '../../services/session-service.js';
import { settingsService } from '../../services/admin-service.js';
import { skeletonTable } from '../../components/states.js';

const profileFields = [
  { name: 'name', label: 'Tên nhà thuốc', required: true, full: true },
  { name: 'phone', label: 'Số điện thoại', type: 'tel', required: true },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'taxCode', label: 'Mã số thuế' },
  { name: 'license', label: 'Số giấy phép GPP' },
  { name: 'address', label: 'Địa chỉ', full: true },
];

const thresholdFields = [
  {
    name: 'lowStockMultiplier',
    label: 'Hệ số ngưỡng cảnh báo tồn',
    type: 'number',
    min: 0.5,
    max: 5,
    step: 0.1,
    required: true,
    help: 'Nhân với ngưỡng tồn tối thiểu của từng thuốc. 1 = giữ nguyên ngưỡng.',
  },
  {
    name: 'expiryWarningDays',
    label: 'Cảnh báo hạn dùng (ngày)',
    type: 'number',
    min: 7,
    max: 365,
    step: 1,
    required: true,
    help: 'Lô còn hạn trong số ngày này được xem là sắp hết hạn.',
  },
  { name: 'tier1', label: 'Mốc hạn dùng 1 (ngày)', type: 'number', min: 1, max: 365, step: 1, required: true },
  { name: 'tier2', label: 'Mốc hạn dùng 2 (ngày)', type: 'number', min: 1, max: 365, step: 1, required: true },
  {
    name: 'tier3',
    label: 'Mốc hạn dùng 3 (ngày)',
    type: 'number',
    min: 1,
    max: 365,
    step: 1,
    required: true,
    help: 'Ba mốc dùng cho trang Hạn sử dụng của kho, phải tăng dần.',
  },
];

const loyaltyFields = [
  {
    name: 'earnAmountPerPoint',
    label: 'Số tiền để được 1 điểm (VNĐ)',
    type: 'number',
    min: 1000,
    max: 10000000,
    step: 1000,
    required: true,
    help: 'Tính trên số tiền khách thực trả, làm tròn xuống. Ví dụ 10.000 = cứ 10.000đ được 1 điểm.',
  },
  {
    name: 'pointValue',
    label: 'Giá trị 1 điểm khi đổi (VNĐ)',
    type: 'number',
    min: 100,
    max: 1000000,
    step: 100,
    required: true,
  },
  {
    name: 'maxRedeemPercent',
    label: 'Tối đa được đổi (% giá trị đơn)',
    type: 'number',
    min: 1,
    max: 100,
    step: 1,
    required: true,
    help: 'Tính trên giá trị đơn sau giảm giá.',
  },
];

const notificationLabels = {
  lowStock: 'Cảnh báo thuốc sắp hết',
  expiring: 'Cảnh báo lô sắp hết hạn',
  newReceipt: 'Thông báo phiếu nhập mới',
  dailyReport: 'Báo cáo doanh thu cuối ngày',
};

const section = (id, title, formBody, submitLabel) =>
  card({
    title,
    body: html`<form data-form="${id}" novalidate class="stack">${formBody}<div><button type="submit" class="btn btn--primary" data-submit>${icon('save', 'icon--sm')} ${submitLabel}</button></div></form>`,
  });

startPage({
  role: 'ADMIN',
  nav: 'settings',
  title: 'Cài đặt',
  render: async ({ content }) => {
    content.innerHTML =
      html`${pageHeading({ title: 'Cài đặt', subtitle: 'Thông tin nhà thuốc, ngưỡng cảnh báo, giao diện và thông báo.' })}${skeletonTable(6)}`.value;
    const settings = await settingsService.get();
    const prefs = preferences.get();

    content.innerHTML = html`
      ${pageHeading({ title: 'Cài đặt', subtitle: 'Thông tin nhà thuốc, ngưỡng cảnh báo, giao diện và thông báo.' })}
      <div class="grid grid--2">
        <div class="stack">
          ${section('profile', 'Hồ sơ nhà thuốc', renderFields(profileFields, settings.pharmacy, 'pf'), 'Lưu hồ sơ')}
          ${section(
            'threshold',
            'Ngưỡng cảnh báo',
            renderFields(
              thresholdFields,
              {
                lowStockMultiplier: settings.lowStockMultiplier,
                expiryWarningDays: settings.expiryWarningDays,
                tier1: settings.expiryTiers[0],
                tier2: settings.expiryTiers[1],
                tier3: settings.expiryTiers[2],
              },
              'th',
            ),
            'Lưu ngưỡng',
          )}
        </div>
        <div class="stack">
          ${section(
            'loyalty',
            'Chương trình tích điểm',
            html`<label class="switch"><span>Bật tích điểm và đổi điểm</span><input type="checkbox" name="enabled" ${settings.loyalty.enabled ? 'checked' : ''} /></label>${renderFields(loyaltyFields, settings.loyalty, 'lo')}`,
            'Lưu chương trình',
          )}
          ${section(
            'appearance',
            'Giao diện',
            html`<div class="form-grid">
              <div class="form-field"><label class="form-label" for="ap-theme">Chủ đề màu</label>
                <select class="form-control" id="ap-theme" name="theme"><option value="light" ${prefs.theme === 'light' ? 'selected' : ''}>Sáng</option><option value="dark" ${prefs.theme === 'dark' ? 'selected' : ''}>Tối</option></select></div>
              <div class="form-field"><label class="form-label" for="ap-density">Mật độ bảng</label>
                <select class="form-control" id="ap-density" name="density"><option value="comfortable" ${prefs.density === 'comfortable' ? 'selected' : ''}>Thoải mái</option><option value="compact" ${prefs.density === 'compact' ? 'selected' : ''}>Gọn</option></select></div>
            </div><p class="form-help">Giao diện được lưu trên trình duyệt này.</p>`,
            'Áp dụng giao diện',
          )}
          ${section(
            'notifications',
            'Tùy chọn thông báo',
            html`<div>${Object.entries(notificationLabels).map(([key, label]) => html`<label class="switch"><span>${label}</span><input type="checkbox" name="${key}" ${settings.notifications[key] ? 'checked' : ''} /></label>`)}</div>`,
            'Lưu tùy chọn',
          )}
        </div>
      </div>
    `.value;

    const bind = (id, handler) => {
      const form = content.querySelector(`[data-form="${id}"]`);
      const submit = form.querySelector('[data-submit]');
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearErrors(form);
        setLoading(submit, true);
        try {
          await handler(form);
        } catch (error) {
          if (error.fieldErrors) showErrors(form, error.fieldErrors, { profile: 'pf', loyalty: 'lo' }[id] || 'th');
          toast.error('Không thể lưu cài đặt', errorMessage(error));
        } finally {
          setLoading(submit, false);
        }
      });
    };

    bind('profile', async (form) => {
      const values = readValues(form, profileFields);
      const errors = validateFields(profileFields, values);
      if (Object.keys(errors).length) return showErrors(form, errors, 'pf');
      await settingsService.update({ pharmacy: values });
      return toast.success('Đã lưu hồ sơ nhà thuốc');
    });
    bind('threshold', async (form) => {
      const values = readValues(form, thresholdFields);
      const errors = validateFields(thresholdFields, values);
      if (Object.keys(errors).length) return showErrors(form, errors, 'th');
      await settingsService.update({
        lowStockMultiplier: values.lowStockMultiplier,
        expiryWarningDays: values.expiryWarningDays,
        expiryTiers: [values.tier1, values.tier2, values.tier3],
      });
      return toast.success('Đã lưu ngưỡng cảnh báo', 'Các cảnh báo tồn kho và hạn dùng sẽ áp dụng ngay.');
    });
    bind('loyalty', async (form) => {
      const values = readValues(form, loyaltyFields);
      const errors = validateFields(loyaltyFields, values);
      if (Object.keys(errors).length) return showErrors(form, errors, 'lo');
      await settingsService.update({
        loyalty: {
          enabled: form.elements.enabled.checked,
          earnAmountPerPoint: values.earnAmountPerPoint,
          pointValue: values.pointValue,
          maxRedeemPercent: values.maxRedeemPercent,
        },
      });
      return toast.success('Đã lưu chương trình tích điểm', 'Áp dụng cho các hóa đơn tiếp theo.');
    });
    bind('appearance', async (form) => {
      preferences.update({ theme: form.elements.theme.value, density: form.elements.density.value });
      window.dispatchEvent(new CustomEvent('pms:theme-changed'));
      await settingsService.update({
        appearance: { theme: form.elements.theme.value, density: form.elements.density.value },
      });
      return toast.success('Đã áp dụng giao diện');
    });
    bind('notifications', async (form) => {
      const notifications = Object.fromEntries(
        Object.keys(notificationLabels).map((key) => [key, form.elements[key].checked]),
      );
      await settingsService.update({ notifications });
      return toast.success('Đã lưu tùy chọn thông báo');
    });
  },
});
