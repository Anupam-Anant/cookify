// Explore page recipe-card carousel arrows.
// Externalized under /js/** (CSP script-src has no 'unsafe-inline', so this cannot be an inline
// <script> body or inline onclick handler) — same pattern as lang-switcher.js / chat.js /
// recipe-detail.js.
(function () {
    var carousel = document.getElementById('ck-recipe-carousel');
    if (!carousel) {
        return;
    }
    var scrollAmount = 260;
    var prev = document.querySelector('.ck-carousel-prev');
    var next = document.querySelector('.ck-carousel-next');
    if (prev) {
        prev.addEventListener('click', function () {
            carousel.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
        });
    }
    if (next) {
        next.addEventListener('click', function () {
            carousel.scrollBy({ left: scrollAmount, behavior: 'smooth' });
        });
    }
})();
