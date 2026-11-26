# Contratto del bootstrap

Contratto stabilito prima dell'implementazione; OpenAPI generata dall'app su `GET /api/openapi`, autenticata. Nessun endpoint contabile.

| Metodo e percorso | Autenticazione / risultato |
| --- | --- |
| GET /api/auth/csrf | Anche anonimo; token CSRF e nome header, sessione tramite cookie HttpOnly |
| POST /api/auth/login | Form URL encoded username/password + header CSRF; 204, 401 credenziali errate, 429 tentativi eccessivi |
| POST /api/auth/logout | Autenticato + CSRF; 204, sessione invalidata |
| GET /api/connection | Autenticato; utente corrente e famiglia corrente verificati dal backend |
| GET /api/families/{id} | Autenticato; solo propria famiglia, 404 per ID non appartenente |
| GET /api/admin/users | Solo amministratore; utenti della propria famiglia |
| POST /api/admin/users | Solo amministratore + CSRF; JSON username/password/role; 201, 400 invalido, 409 duplicato |
| POST /api/admin/users/{id}/disable | Solo amministratore + CSRF; utente della propria famiglia, 204, 404 estraneo, 409 disattivazione propria utenza |

Tutte le altre API richiedono autenticazione, nessun signup pubblico. Login e logout ruotano il token CSRF: recuperarlo di nuovo dopo login. Richieste mutanti senza token valido: 403. Errori JSON con `message`, senza password, stack trace o informazioni bancarie; risposta anonima 401, permesso insufficiente 403. Nessun dato o credenziale reale negli esempi o nei test.
