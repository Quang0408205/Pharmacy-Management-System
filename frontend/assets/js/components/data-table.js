import { html, raw } from '../core/html.js';
import { icon } from '../core/icons.js';
import { debounce, delegate, qs } from '../core/dom.js';
import { formatNumber } from '../core/format.js';
import { emptyState, errorState, skeletonTable } from './states.js';

const renderOptions = (filter, value) => html`<option value="">${filter.allLabel || `${filter.label}: tất cả`}</option>
  ${(filter.options || []).map((option) => html`<option value="${option.value}" ${String(option.value) === String(value ?? '') ? raw('selected') : ''}>${option.label}</option>`)}`;

const renderFilter = (filter, value) => {
  if (filter.type === 'search') {
    return html`<div class="toolbar__search">
      ${icon('search')}
      <input class="form-control" type="search" name="${filter.name}" value="${value ?? ''}" placeholder="${filter.placeholder || 'Tìm kiếm...'}" aria-label="${filter.label || filter.placeholder || 'Tìm kiếm'}" autocomplete="off" />
    </div>`;
  }
  if (filter.type === 'date') {
    return html`<label class="row text-small text-secondary">${filter.label}
      <input class="form-control" type="date" name="${filter.name}" value="${value ?? ''}" />
    </label>`;
  }
  return html`<select class="form-control" name="${filter.name}" aria-label="${filter.label}">${renderOptions(filter, value)}</select>`;
};

const pageWindow = (page, totalPages) => {
  const pages = new Set([1, totalPages, page, page - 1, page + 1]);
  const sorted = [...pages].filter((value) => value >= 1 && value <= totalPages).sort((a, b) => a - b);
  const result = [];
  sorted.forEach((value, index) => {
    if (index > 0 && value - sorted[index - 1] > 1) result.push('gap');
    result.push(value);
  });
  return result;
};

export const createDataTable = ({
  container,
  columns,
  fetch,
  filters = [],
  initialParams = {},
  pageSize = 10,
  pageSizes = [10, 20, 50],
  defaultSort = '',
  rowKey = 'id',
  rowActions = null,
  actionsLabel = 'Thao tác',
  onRowClick = null,
  onAction = null,
  onLoaded = null,
  toolbarExtra = '',
  caption = 'Danh sách dữ liệu',
  emptyTitle = 'Không có dữ liệu',
  emptyMessage = 'Chưa có bản ghi nào để hiển thị.',
  emptyIcon = 'inbox',
  card = true,
}) => {
  const state = {
    params: { page: 1, size: pageSize, sort: defaultSort, ...initialParams },
    rows: [],
    token: 0,
    meta: null,
  };
  const filterNames = filters.map((filter) => filter.name);

  container.innerHTML = html`<div class="${card ? 'card' : ''}">
    ${
      filters.length || toolbarExtra
        ? html`<div class="toolbar" role="search">
          ${filters.map((filter) => renderFilter(filter, state.params[filter.name]))}
          <span class="toolbar__spacer"></span>
          ${toolbarExtra}
        </div>`
        : ''
    }
    <div class="table-wrap" data-region="body" aria-live="polite"></div>
    <div class="pagination" data-region="pagination"></div>
  </div>`.value;

  const bodyRegion = qs('[data-region="body"]', container);
  const paginationRegion = qs('[data-region="pagination"]', container);

  const hasActiveFilters = () => filterNames.some((name) => state.params[name]);

  const renderHead = () => html`<thead>
    <tr>
      ${columns.map((column) => {
        const sortKey = column.sortKey || (column.sortable ? column.key : null);
        const [currentField, direction] = String(state.params.sort || '').split(',');
        const sorted = sortKey && currentField === sortKey;
        const ariaSort = sorted ? (direction === 'desc' ? 'descending' : 'ascending') : 'none';
        const alignClass = column.align === 'right' ? 'num' : '';
        return html`<th scope="col" class="${alignClass} ${column.className || ''}" ${sortKey ? raw(`aria-sort="${ariaSort}"`) : ''}>
          ${sortKey ? html`<button type="button" class="sort-button" data-sort="${sortKey}">${column.label}${icon(sorted ? (direction === 'desc' ? 'chevronDown' : 'chevronUp') : 'sort')}</button>` : column.label}
        </th>`;
      })}
      ${rowActions ? html`<th scope="col" class="actions">${actionsLabel}</th>` : ''}
    </tr>
  </thead>`;

  const renderRows = () => html`<tbody>
    ${state.rows.map((row) => {
      const interactive = onRowClick ? raw('tabindex="0"') : '';
      return html`<tr data-key="${row[rowKey]}" class="${onRowClick ? 'is-clickable' : ''}" ${interactive}>
        ${columns.map((column) => {
          const content = column.render ? column.render(row) : row[column.key];
          return html`<td class="${column.align === 'right' ? 'num' : ''} ${column.className || ''}">${content}</td>`;
        })}
        ${rowActions ? html`<td class="actions">${rowActions(row)}</td>` : ''}
      </tr>`;
    })}
  </tbody>`;

  const renderPagination = () => {
    const meta = state.meta;
    if (!meta || !meta.total) {
      paginationRegion.classList.add('hidden');
      return;
    }
    paginationRegion.classList.remove('hidden');
    const start = (meta.page - 1) * meta.size + 1;
    const end = Math.min(meta.page * meta.size, meta.total);
    paginationRegion.innerHTML = html`
      <span>Hiển thị ${formatNumber(start)}–${formatNumber(end)} trong ${formatNumber(meta.total)} kết quả</span>
      <nav class="pagination__pages" aria-label="Phân trang">
        <button type="button" class="pagination__button" data-page="${meta.page - 1}" ${meta.page <= 1 ? raw('disabled') : ''} aria-label="Trang trước">${icon('chevronLeft', 'icon--sm')}</button>
        ${pageWindow(meta.page, meta.totalPages).map((value) =>
          value === 'gap'
            ? html`<span aria-hidden="true">…</span>`
            : html`<button type="button" class="pagination__button" data-page="${value}" ${value === meta.page ? raw('aria-current="page"') : ''}>${value}</button>`,
        )}
        <button type="button" class="pagination__button" data-page="${meta.page + 1}" ${meta.page >= meta.totalPages ? raw('disabled') : ''} aria-label="Trang sau">${icon('chevronRight', 'icon--sm')}</button>
      </nav>
      <label class="row text-small">Mỗi trang
        <select class="form-control" data-page-size aria-label="Số dòng mỗi trang">
          ${pageSizes.map((size) => html`<option value="${size}" ${size === meta.size ? raw('selected') : ''}>${size}</option>`)}
        </select>
      </label>
    `.value;
  };

  const load = async () => {
    state.token += 1;
    const token = state.token;
    bodyRegion.innerHTML = skeletonTable(Math.min(state.params.size, 8)).value;
    paginationRegion.classList.add('hidden');
    try {
      const result = await fetch({ ...state.params });
      if (token !== state.token) return;
      state.rows = result.items;
      state.meta = result;
      state.params.page = result.page;
      if (!result.items.length) {
        bodyRegion.innerHTML = emptyState({
          title: hasActiveFilters() ? 'Không tìm thấy kết quả phù hợp' : emptyTitle,
          message: hasActiveFilters() ? 'Thử thay đổi từ khóa hoặc bộ lọc.' : emptyMessage,
          iconName: hasActiveFilters() ? 'search' : emptyIcon,
          action: hasActiveFilters()
            ? html`<button type="button" class="btn btn--secondary btn--sm" data-clear-filters>Xóa bộ lọc</button>`
            : '',
        }).value;
        paginationRegion.classList.add('hidden');
      } else {
        bodyRegion.innerHTML =
          html`<table class="table"><caption class="visually-hidden">${caption}</caption>${renderHead()}${renderRows()}</table>`.value;
        renderPagination();
      }
      if (onLoaded) onLoaded(result);
    } catch (error) {
      if (token !== state.token) return;
      bodyRegion.innerHTML = errorState(error).value;
    }
  };

  const setParams = (patch, { resetPage = true } = {}) => {
    Object.assign(state.params, patch);
    if (resetPage && !('page' in patch)) state.params.page = 1;
    return load();
  };

  const syncControls = () => {
    filterNames.forEach((name) => {
      const control = container.querySelector(`[name="${name}"]`);
      if (control) control.value = state.params[name] ?? '';
    });
  };

  const searchInput = debounce((target) => setParams({ [target.name]: target.value.trim() }), 300);
  const disposers = [];

  disposers.push(
    delegate(container, 'input', '.toolbar input[type="search"]', (event, target) => searchInput(target)),
    delegate(container, 'change', '.toolbar select, .toolbar input[type="date"]', (event, target) =>
      setParams({ [target.name]: target.value }),
    ),
    delegate(container, 'click', '[data-page]', (event, target) => {
      if (target.disabled) return;
      setParams({ page: Number(target.dataset.page) });
    }),
    delegate(container, 'change', '[data-page-size]', (event, target) => setParams({ size: Number(target.value) })),
    delegate(container, 'click', '[data-sort]', (event, target) => {
      const field = target.dataset.sort;
      const [current, direction] = String(state.params.sort || '').split(',');
      setParams({ sort: `${field},${current === field && direction !== 'desc' ? 'desc' : 'asc'}` });
    }),
    delegate(container, 'click', '[data-clear-filters]', () => {
      const cleared = Object.fromEntries(filterNames.map((name) => [name, '']));
      Object.assign(state.params, cleared);
      syncControls();
      load();
    }),
    delegate(container, 'click', '[data-retry]', () => load()),
  );

  const findRow = (element) => {
    const tr = element.closest('tr[data-key]');
    return tr ? state.rows.find((row) => String(row[rowKey]) === tr.dataset.key) : null;
  };

  disposers.push(
    delegate(container, 'click', '[data-row-action]', (event, target) => {
      event.stopPropagation();
      const row = findRow(target);
      if (row && onAction) onAction(target.dataset.rowAction, row, target);
    }),
    delegate(container, 'click', '.toolbar [data-action]', (event, target) => {
      if (onAction) onAction(target.dataset.action, null, target);
    }),
  );

  if (onRowClick) {
    disposers.push(
      delegate(container, 'click', 'tbody tr[data-key]', (event, target) => {
        if (event.target instanceof Element && event.target.closest('button, a, input, select, label')) return;
        const row = findRow(target);
        if (row) onRowClick(row);
      }),
      delegate(container, 'keydown', 'tbody tr[data-key]', (event, target) => {
        if (event.key !== 'Enter' || event.target !== target) return;
        const row = findRow(target);
        if (row) onRowClick(row);
      }),
    );
  }

  return {
    reload: load,
    setParams,
    get params() {
      return state.params;
    },
    get rows() {
      return state.rows;
    },
    setFilterOptions(name, options) {
      const filter = filters.find((item) => item.name === name);
      const select = container.querySelector(`select[name="${name}"]`);
      if (!filter || !select) return;
      filter.options = options;
      const current = state.params[name] ?? '';
      select.innerHTML = renderOptions(filter, current).value;
      select.value = current;
    },
    destroy() {
      disposers.forEach((dispose) => dispose());
    },
  };
};
