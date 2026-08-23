/* Shared prototype state: language, touch/desktop density, and the product
   category tree. Persisted to localStorage so a choice survives a reload of
   the same file; browsers scope localStorage per file:// path, so it will not
   always sync live across the four separate .html files - the real app will
   share this through the backend instead. */
window.ERP = (function () {
  var LANG_KEY = 'erp_proto_lang';
  var DENSITY_KEY = 'erp_proto_density';
  var CATEGORY_KEY = 'erp_proto_categories';
  var listeners = [];

  var defaultCategoryTree = {
    label: 'All',
    children: [
      { label: 'Electronics' },
      { label: 'Clothing' },
      { label: 'Grocery' },
      { label: 'Hardware' },
      { label: 'Reload Cards', children: [
          { label: 'Dialog', children: [ {label:'Rs. 50'}, {label:'Rs. 100'}, {label:'Rs. 200'}, {label:'Rs. 500'} ] },
          { label: 'Airtel', children: [ {label:'Rs. 50'}, {label:'Rs. 100'}, {label:'Rs. 300'} ] },
          { label: 'Hutch', children: [ {label:'Rs. 100'}, {label:'Rs. 200'} ] },
          { label: 'Mobitel', children: [ {label:'Rs. 50'}, {label:'Rs. 100'}, {label:'Rs. 500'} ] }
        ]
      },
      { label: 'Beverages' },
      { label: 'Snacks' },
      { label: 'Phone Accessories' }
    ]
  };

  var dict = {
    en: {
      language: 'Language', touch: 'Touch', desktop: 'Desktop', settings: 'Settings',
      module_launcher: 'Module launcher', search_modules: 'Search modules...',
      products: 'Products', inventory: 'Inventory', purchasing: 'Purchasing', sales: 'Sales',
      pos: 'Point of Sale', ecommerce: 'E-Commerce', customers: 'Customers', suppliers: 'Suppliers',
      shipping: 'Shipping', payments: 'Payments', accounting: 'Accounting', promotions: 'Promotions',
      reporting: 'Reporting', notifications: 'Notifications', audit_logs: 'Audit Logs',
      new_product: 'New product', import: 'Import', cancel: 'Cancel', save_changes: 'Save changes',
      all_statuses: 'All statuses', active: 'Active', draft: 'Draft', archived: 'Archived',
      search_products: 'Search by name, SKU, or barcode...', product_col: 'Product', sku_col: 'SKU',
      unit_col: 'Unit', buying_price_col: 'Buying price', selling_price_col: 'Selling price',
      discount_col: 'Discount', stock_col: 'Stock', stock_value_col: 'Stock value', status_col: 'Status',
      actions_col: 'Actions', category_col: 'Category', add_category: '+ Add category',
      all_items: 'All items', current_sale: 'Current sale', apply_discount: 'Apply discount',
      apply: 'Apply', charge: 'Charge', cash: 'Cash', card: 'Card',
      general_information: 'General information', pricing: 'Pricing', inventory_tracking: 'Inventory tracking',
      variants: 'Variants', attributes: 'Attributes', record_info: 'Record info', images: 'Images',
      add_attribute: '+ Add attribute', add_variant: '+ Add variant', brand_theme: 'Brand theme',
      appearance: 'Appearance', theme_color: 'Brand color', default_appearance: 'Default appearance',
      light: 'Light', dark: 'Dark', preferences: 'Preferences', back: 'Back', settings_title: 'Settings',
      settings_intro: 'These preferences apply across every module.',
      operations: 'Operations', business_partners: 'Business partners', finance: 'Finance',
      insight_system: 'Insight & system', good_afternoon: 'Good afternoon', dashboard_settings: 'Dashboard settings',
      recent: 'Recent'
    },
    si: {
      language: 'භාෂාව', touch: 'ස්පර්ශ', desktop: 'ඩෙස්ක්ටොප්', settings: 'සැකසුම්',
      module_launcher: 'මොඩියුල දියත් කිරීම', search_modules: 'මොඩියුල සොයන්න...',
      products: 'නිෂ්පාදන', inventory: 'තොග', purchasing: 'මිලදී ගැනීම්', sales: 'විකුණුම්',
      pos: 'විකුණුම් ස්ථානය', ecommerce: 'විද්‍යුත් වාණිජ්‍යය', customers: 'පාරිභෝගිකයන්', suppliers: 'සැපයුම්කරුවන්',
      shipping: 'නැව්ගත කිරීම', payments: 'ගෙවීම්', accounting: 'ගිණුම්කරණය', promotions: 'උසස් කිරීම්',
      reporting: 'වාර්තා', notifications: 'දැනුම්දීම්', audit_logs: 'විගණන සටහන්',
      new_product: 'නව නිෂ්පාදනය', import: 'ආනයනය', cancel: 'අවලංගු කරන්න', save_changes: 'වෙනස්කම් සුරකින්න',
      all_statuses: 'සියලු තත්ත්ව', active: 'සක්‍රීය', draft: 'කෙටුම්පත', archived: 'ලේඛනගත කළ',
      search_products: 'නම, SKU, හෝ බාර්කෝඩ් මගින් සොයන්න...', product_col: 'නිෂ්පාදනය', sku_col: 'SKU',
      unit_col: 'ඒකකය', buying_price_col: 'මිලදී ගැනීමේ මිල', selling_price_col: 'විකුණුම් මිල',
      discount_col: 'වට්ටම', stock_col: 'තොගය', stock_value_col: 'තොග වටිනාකම', status_col: 'තත්ත්වය',
      actions_col: 'ක්‍රියා', category_col: 'ප්‍රවර්ගය', add_category: '+ ප්‍රවර්ගයක් එක් කරන්න',
      all_items: 'සියලු අයිතම', current_sale: 'වත්මන් විකිණීම', apply_discount: 'වට්ටම යොදන්න',
      apply: 'යොදන්න', charge: 'අය කරන්න', cash: 'මුදල්', card: 'කාඩ්පත',
      general_information: 'සාමාන්‍ය තොරතුරු', pricing: 'මිල ගණන්', inventory_tracking: 'තොග නිරීක්ෂණය',
      variants: 'ප්‍රභේද', attributes: 'ගුණාංග', record_info: 'වාර්තා තොරතුරු', images: 'රූප',
      add_attribute: '+ ගුණාංගයක් එක් කරන්න', add_variant: '+ ප්‍රභේදයක් එක් කරන්න', brand_theme: 'සන්නාම තේමාව',
      appearance: 'පෙනුම', theme_color: 'සන්නාම වර්ණය', default_appearance: 'පෙරනිමි පෙනුම',
      light: 'ආලෝකමත්', dark: 'අඳුරු', preferences: 'අභිප්‍රේත', back: 'ආපසු', settings_title: 'සැකසුම්',
      settings_intro: 'මෙම අභිප්‍රේත සියලුම මොඩියුල හරහා අදාළ වේ.', recent: 'මෑත'
    },
    ta: {
      language: 'மொழி', touch: 'தொடு', desktop: 'டெஸ்க்டாப்', settings: 'அமைப்புகள்',
      module_launcher: 'தொகுதி துவக்கி', search_modules: 'தொகுதிகளைத் தேடு...',
      products: 'தயாரிப்புகள்', inventory: 'சரக்கு', purchasing: 'கொள்முதல்', sales: 'விற்பனை',
      pos: 'விற்பனை மையம்', ecommerce: 'இ-வணிகம்', customers: 'வாடிக்கையாளர்கள்', suppliers: 'சப்ளையர்கள்',
      shipping: 'அனுப்புதல்', payments: 'கொடுப்பனவுகள்', accounting: 'கணக்கியல்', promotions: 'விளம்பரங்கள்',
      reporting: 'அறிக்கைகள்', notifications: 'அறிவிப்புகள்', audit_logs: 'தணிக்கை பதிவுகள்',
      new_product: 'புதிய தயாரிப்பு', import: 'இறக்குமதி', cancel: 'ரத்து செய்', save_changes: 'மாற்றங்களை சேமி',
      all_statuses: 'அனைத்து நிலைகள்', active: 'செயலில்', draft: 'வரைவு', archived: 'காப்பகப்படுத்தப்பட்டது',
      search_products: 'பெயர், SKU, அல்லது பார்கோட் மூலம் தேடு...', product_col: 'தயாரிப்பு', sku_col: 'SKU',
      unit_col: 'அலகு', buying_price_col: 'வாங்கும் விலை', selling_price_col: 'விற்பனை விலை',
      discount_col: 'தள்ளுபடி', stock_col: 'சரக்கு', stock_value_col: 'சரக்கு மதிப்பு', status_col: 'நிலை',
      actions_col: 'செயல்கள்', category_col: 'வகை', add_category: '+ வகையைச் சேர்',
      all_items: 'அனைத்து பொருட்கள்', current_sale: 'தற்போதைய விற்பனை', apply_discount: 'தள்ளுபடி பயன்படுத்து',
      apply: 'பயன்படுத்து', charge: 'கட்டணம்', cash: 'பணம்', card: 'அட்டை',
      general_information: 'பொது தகவல்', pricing: 'விலை நிர்ணயம்', inventory_tracking: 'சரக்கு கண்காணிப்பு',
      variants: 'மாறுபாடுகள்', attributes: 'பண்புகள்', record_info: 'பதிவு தகவல்', images: 'படங்கள்',
      add_attribute: '+ பண்பு சேர்', add_variant: '+ மாறுபாடு சேர்', brand_theme: 'பிராண்ட் தீம்',
      appearance: 'தோற்றம்', theme_color: 'பிராண்ட் நிறம்', default_appearance: 'இயல்புநிலை தோற்றம்',
      light: 'வெளிச்சம்', dark: 'இருள்', preferences: 'விருப்பங்கள்', back: 'பின்', settings_title: 'அமைப்புகள்',
      settings_intro: 'இந்த விருப்பங்கள் அனைத்து தொகுதிகளிலும் பொருந்தும்.', recent: 'சமீபத்திய'
    }
  };

  function safeGet(key) { try { return localStorage.getItem(key); } catch (e) { return null; } }
  function safeSet(key, val) { try { localStorage.setItem(key, val); } catch (e) {} }

  function getLang() { return safeGet(LANG_KEY) || 'en'; }
  function setLang(lang) { safeSet(LANG_KEY, lang); }
  function t(key) { return (dict[getLang()] || dict.en)[key] || key; }

  function getDensity() { return safeGet(DENSITY_KEY) || 'touch'; }
  function setDensity(mode) { safeSet(DENSITY_KEY, mode); }

  function loadCategoryTree() {
    var raw = safeGet(CATEGORY_KEY);
    if (raw) { try { return JSON.parse(raw); } catch (e) {} }
    return JSON.parse(JSON.stringify(defaultCategoryTree));
  }
  function saveCategoryTree(tree) { safeSet(CATEGORY_KEY, JSON.stringify(tree)); }

  function ensureCategories(names) {
    var tree = loadCategoryTree();
    tree.children = tree.children || [];
    var existing = tree.children.map(function (c) { return c.label; });
    var changed = false;
    names.forEach(function (name) {
      if (existing.indexOf(name) === -1) {
        tree.children.push({ label: name });
        existing.push(name);
        changed = true;
      }
    });
    if (changed) saveCategoryTree(tree);
    return tree;
  }

  function onLanguageChange(cb) { listeners.push(cb); }

  function applyI18n(lang) {
    document.documentElement.setAttribute('lang', lang);
    var d = dict[lang] || dict.en;
    document.querySelectorAll('[data-i18n]').forEach(function (el) {
      var key = el.getAttribute('data-i18n');
      if (d[key]) el.textContent = d[key];
    });
    document.querySelectorAll('[data-i18n-placeholder]').forEach(function (el) {
      var key = el.getAttribute('data-i18n-placeholder');
      if (d[key]) el.setAttribute('placeholder', d[key]);
    });
    document.querySelectorAll('[data-name-en]').forEach(function (el) {
      var key = 'name' + lang.charAt(0).toUpperCase() + lang.slice(1);
      el.textContent = el.dataset[key] || el.dataset.nameEn;
    });
    document.querySelectorAll('.lang-select').forEach(function (sel) { sel.value = lang; });
    listeners.forEach(function (cb) { cb(lang); });
  }

  function wireLangSelect(sel) {
    sel.value = getLang();
    sel.addEventListener('change', function () {
      setLang(sel.value);
      applyI18n(sel.value);
    });
  }

  function wireDensityToggle(touchBtn, pcBtn) {
    function set(mode) {
      document.body.classList.toggle('pc-mode', mode === 'pc');
      touchBtn.classList.toggle('active', mode !== 'pc');
      pcBtn.classList.toggle('active', mode === 'pc');
      setDensity(mode);
    }
    set(getDensity());
    touchBtn.addEventListener('click', function () { set('touch'); });
    pcBtn.addEventListener('click', function () { set('pc'); });
  }

  function initPage() { applyI18n(getLang()); }

  function renderCategoryNav(breadcrumbEl, chipsEl, opts) {
    opts = opts || {};
    var tree = loadCategoryTree();
    var path = [tree];

    function render() {
      var node = path[path.length - 1];
      breadcrumbEl.innerHTML = '';
      path.forEach(function (n, i) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'crumb-btn' + (i === path.length - 1 ? ' current' : '');
        btn.textContent = i === 0 ? t('all_items') : n.label;
        btn.addEventListener('click', function () { path = path.slice(0, i + 1); render(); });
        breadcrumbEl.appendChild(btn);
        if (i < path.length - 1) {
          var sep = document.createElement('span');
          sep.className = 'crumb-sep';
          sep.textContent = '/';
          breadcrumbEl.appendChild(sep);
        }
      });

      chipsEl.innerHTML = '';
      (node.children || []).forEach(function (child) {
        var chip = document.createElement('button');
        chip.type = 'button';
        chip.className = 'cat-chip' + (child.children ? ' has-children' : '');
        chip.textContent = child.label;
        chip.addEventListener('click', function () {
          if (child.children) {
            path.push(child);
            render();
          } else {
            chipsEl.querySelectorAll('.cat-chip').forEach(function (c) { c.classList.remove('active'); });
            chip.classList.add('active');
            if (opts.onLeafSelect) opts.onLeafSelect(child.label, path);
          }
        });
        chipsEl.appendChild(chip);
      });

      var addChip = document.createElement('button');
      addChip.type = 'button';
      addChip.className = 'cat-chip add-cat-chip';
      addChip.textContent = t('add_category');
      addChip.addEventListener('click', function () {
        var input = document.createElement('input');
        input.type = 'text';
        input.className = 'cat-add-input';
        input.placeholder = t('add_category');
        chipsEl.replaceChild(input, addChip);
        input.focus();
        var done = false;
        function commit() {
          if (done) return;
          done = true;
          var name = input.value.trim();
          if (name) {
            var node2 = path[path.length - 1];
            node2.children = node2.children || [];
            node2.children.push({ label: name });
            saveCategoryTree(tree);
          }
          render();
        }
        input.addEventListener('keydown', function (e) { if (e.key === 'Enter') commit(); if (e.key === 'Escape') render(); });
        input.addEventListener('blur', commit);
      });
      chipsEl.appendChild(addChip);
    }
    render();
    onLanguageChange(function () { render(); });
    return { refresh: render };
  }

  return {
    getLang: getLang, setLang: setLang, t: t, applyI18n: applyI18n, wireLangSelect: wireLangSelect,
    onLanguageChange: onLanguageChange, getDensity: getDensity, setDensity: setDensity,
    wireDensityToggle: wireDensityToggle, loadCategoryTree: loadCategoryTree, saveCategoryTree: saveCategoryTree,
    ensureCategories: ensureCategories, renderCategoryNav: renderCategoryNav, initPage: initPage
  };
})();
