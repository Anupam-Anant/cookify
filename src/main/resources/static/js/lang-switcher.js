// Language switcher + Google Translate widget init.
// Externalized so it loads under the strict CSP (script-src 'self' ... — no 'unsafe-inline'):
// a real browser blocks inline <script> bodies and inline on* handlers, so this file is served
// from /js/** (same-origin, permitted by SecurityConfig) instead.
//
// Load order matters: this file must load BEFORE the external Google loader
// (https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit), since
// that loader invokes window.googleTranslateElementInit as soon as it finishes loading.

function ckSetLanguage(lang) {
    var host = window.location.hostname;
    document.cookie = 'googtrans=/en/' + lang + '; path=/';
    document.cookie = 'googtrans=/en/' + lang + '; path=/; domain=' + host;
    window.location.reload();
}

window.googleTranslateElementInit = function () {
    new google.translate.TranslateElement(
        { pageLanguage: 'en', includedLanguages: 'en,hi,zh-CN,ru,es,fr', autoDisplay: false },
        'google_translate_element');
};

(function () {
    var select = document.getElementById('ck-lang-select');
    if (select) {
        select.addEventListener('change', function () {
            ckSetLanguage(select.value);
        });
    }
})();
