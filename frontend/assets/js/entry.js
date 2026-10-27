import { authService } from './services/auth-service.js';
import { goHome, goToLogin, preferences } from './services/session-service.js';

preferences.apply();

authService
  .currentUser()
  .then((user) => goHome(user.role))
  .catch(() => goToLogin());
