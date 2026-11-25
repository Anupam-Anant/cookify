// Share-link copy-to-clipboard handler for the recipe detail page.
// Externalized (CSP script-src has no 'unsafe-inline' — inline <script> bodies and inline on*
// handlers are blocked by the browser); served from /js/** (same-origin, CSP-permitted).
(function () {
    var btn = document.getElementById('ck-share-btn');
    if (!btn) {
        return;
    }
    btn.addEventListener('click', function () {
        var path = btn.getAttribute('data-share-url');
        var url = window.location.origin + path;
        var feedback = document.getElementById('ck-share-feedback');
        function showFeedback() {
            if (!feedback) {
                return;
            }
            feedback.hidden = false;
            setTimeout(function () { feedback.hidden = true; }, 2000);
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(url).then(showFeedback, showFeedback);
        } else {
            window.prompt('Copy this link:', url);
        }
    });
})();
