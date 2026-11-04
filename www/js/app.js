(() => {
  const main = document.querySelector("main");
  const homeContent = main.innerHTML;
  let currentPage = "home";
  let currentAddress = "";
  const NativeBrowser = window.Capacitor?.registerPlugin?.("NativeBrowser");

  async function openNativeBrowser(url = "https://www.google.com") {
    if (!NativeBrowser) {
      alert("Native browser is available only inside the Android APK.");
      return;
    }

    try {
      await NativeBrowser.openBrowser({ url });
    } catch (error) {
      alert("Could not open browser: " + error.message);
    }
  }
  const pages = {
    browser: `
      <h2 class="page-title">Ultra Browser</h2>
      <p class="page-subtitle">Browse the web from one place.</p>
      <form class="browser-address" id="browserForm">
        <input id="browserInput" placeholder="Search or enter website URL"
          autocomplete="url" value="">
        <button type="submit">GO</button>
      </form>
      <div class="content-panel">
        <h3>Browser workspace</h3>
        <p id="browserStatus">Your browser workspace is ready.</p>
        <div class="status-note">
          The native Android WebView connection is the next development
          stage. This screen is currently the browser interface, not yet
          a live website renderer.
        </div>
      </div>
      <div class="content-panel">
        <h3>Browser features planned</h3>
        <p>Tabs · Back and forward · Refresh · Cookies · Private browsing</p>
      </div>`,
    downloads: `
      <h2 class="page-title">Downloads</h2>
      <p class="page-subtitle">Manage your downloaded files.</p>
      <div class="content-panel">
        <div class="empty-state">
          <div class="empty-icon">↓</div>
          <h4>No downloads yet</h4>
          <p>Your download history will appear here.</p>
        </div>
      </div>
      <div class="content-panel">
        <h3>Download manager</h3>
        <p>Download queue, progress, pause, resume and file details will be
        connected to Android's native download system.</p>
      </div>`,
    files: `
      <h2 class="page-title">My Files</h2>
      <p class="page-subtitle">Your downloaded content in one place.</p>
      <div class="content-panel">
        <h3>File library</h3>
        <p>Files will be listed here after Android storage integration.</p>
      </div>
      <div class="content-panel">
        <h3>Categories</h3>
        <p>Videos · Music · Images · Documents · Other files</p>
      </div>`,
    settings: `
      <h2 class="page-title">Settings</h2>
      <p class="page-subtitle">Customize your Ultra Downloader.</p>
      <div class="content-panel">
        <h3>Appearance</h3>
        <div class="setting-row"><span>Dark theme</span><strong>ON</strong></div>
        <div class="setting-row"><span>Cyan accent</span><strong>ON</strong></div>
      </div>
      <div class="content-panel">
        <h3>Browser preferences</h3>
        <label class="setting-row"><span>Save browsing history</span>
          <input type="checkbox" checked></label>
        <label class="setting-row"><span>JavaScript</span>
          <input type="checkbox" checked></label>
      </div>
      <div class="content-panel">
        <h3>About</h3>
        <p>Ultra Downloader · Version 1.0.0</p>
        <p>Built with local web files and Capacitor.</p>
      </div>`
  };

  function navigate(page) {
    currentPage = page;
    main.innerHTML = page === "home" ? homeContent : (pages[page] || homeContent);

    document.querySelectorAll(".nav-item").forEach(button => {
      button.classList.toggle("active", button.dataset.action === page);
    });

    if (page === "browser") {
      const input = document.getElementById("browserInput");
      if (input) input.value = currentAddress;
    }
    window.scrollTo(0, 0);
  }

  document.addEventListener("click", event => {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    event.preventDefault();
    navigate(button.dataset.action);
  });

  document.addEventListener("submit", event => {
    if (event.target.id !== "searchForm" &&
        event.target.id !== "browserForm") return;

    event.preventDefault();
    const input = event.target.querySelector("input");
    const value = input.value.trim();
    if (!value) return;

    currentAddress = value;
    navigate("browser");
    openNativeBrowser(currentAddress);
  });

  navigate("home");
})();
