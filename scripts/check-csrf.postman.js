pm.test('Il contratto CSRF e disponibile senza autenticazione', () => {
  pm.response.to.have.status(200);
  const body = pm.response.json();
  pm.expect(body.headerName).to.equal('X-CSRF-TOKEN');
  pm.expect(body.token).to.be.a('string').and.not.empty;
});
pm.test('Le risposte di sessione non sono memorizzabili in cache', () => {
  pm.expect(pm.response.headers.get('Cache-Control')).to.include('no-store');
});
