import { startPage } from '../../components/page.js';
import { mountLoyaltyPage } from '../shared/loyalty-views.js';

startPage({
  role: 'ADMIN',
  nav: 'loyalty',
  title: 'Khách thân thiết',
  render: ({ content }) => mountLoyaltyPage({ content, role: 'ADMIN' }),
});
