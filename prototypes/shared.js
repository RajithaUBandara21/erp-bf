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
      recent: 'Recent',
      sign_in: 'Sign in', sign_in_desc: 'Sign in to your Universal ERP workspace',
      sign_up: 'Create your workspace', sign_up_desc: 'Set up a new organization and admin account',
      create_account: 'Create account', email: 'Email', password: 'Password',
      confirm_password: 'Confirm password', full_name: 'Full name', organization_name: 'Organization name',
      forgot_password: 'Forgot password?', remember_me: 'Remember me',
      dont_have_account: "Don't have an account?", already_have_account: 'Already have an account?',
      invalid_credentials: 'Invalid email or password.', weak_password: 'Password must be at least 8 characters.',
      passwords_dont_match: 'Passwords do not match.', agree_terms_prefix: 'I agree to the',
      terms_of_service: 'Terms of Service', and_word: 'and', privacy_policy: 'Privacy Policy',
      sessions_title: 'Active sessions', sessions_intro: 'Devices currently signed in to your account.',
      this_device: 'This device', revoke: 'Revoke', revoke_all_others: 'Revoke all other sessions',
      last_active: 'Last active', signed_in_as: 'Signed in as',
      roles_title: 'Roles & permissions',
      roles_intro: 'Roles bundle permissions. Assign a role to a user to grant everything it allows.',
      new_role: 'New role', role_col: 'Role', description_col: 'Description', members_col: 'Members', type_col: 'Type',
      system_role: 'System', custom_role: 'Custom', permissions: 'Permissions', module_col: 'Module',
      scope_tenant: 'Scope: entire tenant',
      system_role_note: 'This is a built-in system role. Its permissions are fixed and it cannot be deleted. Duplicate it to make an editable copy.',
      perm_view: 'View', perm_create: 'Create', perm_edit: 'Edit', perm_delete: 'Delete', perm_approve: 'Approve',
      perm_legend: 'View lets a user open records. Create, Edit, and Delete change them. Approve confirms documents such as orders and invoices.',
      role_info: 'Role info', role_name_label: 'Role name', scope_label: 'Scope',
      scope_hint: 'Branch and store level roles are planned for a later release.',
      members: 'Members', members_empty: 'No users have this role yet.', add_member: 'Add member',
      administration: 'Administration',
      audit_intro: 'Every recorded action across identity, security, and business data - who did what, when, and what changed.',
      search_audit_logs: 'Search by actor, entity, or action...', entity_col: 'Entity', actor_col: 'Actor',
      action_col: 'Action', timestamp_col: 'Timestamp', organization_col: 'Organization', diff_col: 'Details',
      all_entities: 'All entities', all_actions: 'All actions', all_actors: 'All actors',
      date_from: 'From', date_to: 'To', view_diff: 'View details', diff_title: 'Change details',
      before_label: 'Before', after_label: 'After', close: 'Close',
      no_audit_results: 'No audit log entries match your filters.', unknown_actor: 'Unknown'
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
      settings_intro: 'මෙම අභිප්‍රේත සියලුම මොඩියුල හරහා අදාළ වේ.', recent: 'මෑත',
      operations: 'මෙහෙයුම්', business_partners: 'ව්‍යාපාරික හවුල්කරුවන්', finance: 'මූල්‍ය',
      insight_system: 'තීක්ෂ්ණ බුද්ධිය සහ පද්ධතිය',
      sign_in: 'පිවිසෙන්න', sign_in_desc: 'ඔබගේ Universal ERP කාර්යාලයට පිවිසෙන්න',
      sign_up: 'ඔබගේ කාර්යාලය සාදන්න', sign_up_desc: 'නව සංවිධානයක් සහ පරිපාලක ගිණුමක් සකසන්න',
      create_account: 'ගිණුම සාදන්න', email: 'විද්‍යුත් තැපෑල', password: 'මුරපදය',
      confirm_password: 'මුරපදය තහවුරු කරන්න', full_name: 'සම්පූර්ණ නම', organization_name: 'සංවිධානයේ නම',
      forgot_password: 'මුරපදය අමතකද?', remember_me: 'මාව මතක තබාගන්න',
      dont_have_account: 'ගිණුමක් නැද්ද?', already_have_account: 'දැනටමත් ගිණුමක් තිබේද?',
      invalid_credentials: 'වැරදි විද්‍යුත් තැපෑලක් හෝ මුරපදයක්.', weak_password: 'මුරපදය අවම වශයෙන් අක්ෂර 8ක් විය යුතුය.',
      passwords_dont_match: 'මුරපද නොගැලපේ.', agree_terms_prefix: 'මම එකඟ වෙමි',
      terms_of_service: 'සේවා කොන්දේසි', and_word: 'සහ', privacy_policy: 'පෞද්ගලිකත්ව ප්‍රතිපත්තිය',
      sessions_title: 'සක්‍රිය සැසි', sessions_intro: 'ඔබගේ ගිණුමට දැනට පිවිසී ඇති උපාංග.',
      this_device: 'මෙම උපාංගය', revoke: 'අවලංගු කරන්න', revoke_all_others: 'අනෙකුත් සියලුම සැසි අවලංගු කරන්න',
      last_active: 'අවසන් වරට සක්‍රිය වූයේ', signed_in_as: 'ලෙස පිවිසී ඇත',
      roles_title: 'භූමිකා සහ අවසර',
      roles_intro: 'භූමිකා අවසර එකතු කරයි. පරිශීලකයෙකුට භූමිකාවක් පවරා එහි සියලු අවසර ලබා දෙන්න.',
      new_role: 'නව භූමිකාව', role_col: 'භූමිකාව', description_col: 'විස්තරය', members_col: 'සාමාජිකයන්', type_col: 'වර්ගය',
      system_role: 'පද්ධතිය', custom_role: 'අභිරුචි', permissions: 'අවසර', module_col: 'මොඩියුලය',
      scope_tenant: 'විෂය පථය: මුළු ආයතනය',
      system_role_note: 'මෙය බිල්ට්-ඉන් පද්ධති භූමිකාවකි. එහි අවසර ස්ථිර වන අතර එය මැකිය නොහැක. සංස්කරණය කළ හැකි පිටපතක් සෑදීමට එය අනුපිටපත් කරන්න.',
      perm_view: 'බලන්න', perm_create: 'සාදන්න', perm_edit: 'සංස්කරණය', perm_delete: 'මකන්න', perm_approve: 'අනුමත කරන්න',
      perm_legend: 'බැලීම මගින් පරිශීලකයාට වාර්තා විවෘත කළ හැක. සෑදීම, සංස්කරණය සහ මැකීම ඒවා වෙනස් කරයි. අනුමත කිරීම ඇණවුම් සහ ඉන්වොයිසි වැනි ලේඛන තහවුරු කරයි.',
      role_info: 'භූමිකා තොරතුරු', role_name_label: 'භූමිකාවේ නම', scope_label: 'විෂය පථය',
      scope_hint: 'ශාඛා සහ වෙළඳසැල් මට්ටමේ භූමිකා පසුකාලීන නිකුතුවක් සඳහා සැලසුම් කර ඇත.',
      members: 'සාමාජිකයන්', members_empty: 'තවම කිසිදු පරිශීලකයෙකුට මෙම භූමිකාව නොමැත.', add_member: 'සාමාජිකයෙකු එක් කරන්න',
      administration: 'පරිපාලනය',
      audit_intro: 'අනන්‍යතාව, ආරක්ෂාව සහ ව්‍යාපාරික දත්ත පුරා සටහන් වූ සෑම ක්‍රියාවක්ම - කවුරුන් කුමක් කළේද, කවදාද, සහ කුමක් වෙනස් වූයේද.',
      search_audit_logs: 'පරිශීලකයා, අයිතමය, හෝ ක්‍රියාව අනුව සොයන්න...', entity_col: 'අයිතමය', actor_col: 'පරිශීලකයා',
      action_col: 'ක්‍රියාව', timestamp_col: 'වේලාව', organization_col: 'සංවිධානය', diff_col: 'විස්තර',
      all_entities: 'සියලු අයිතම', all_actions: 'සියලු ක්‍රියා', all_actors: 'සියලු පරිශීලකයන්',
      date_from: 'සිට', date_to: 'දක්වා', view_diff: 'විස්තර බලන්න', diff_title: 'වෙනස්කම් විස්තර',
      before_label: 'පෙර', after_label: 'පසු', close: 'වසන්න',
      no_audit_results: 'ඔබගේ පෙරහන් වලට ගැලපෙන විගණන සටහන් නොමැත.', unknown_actor: 'නොදන්නා'
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
      settings_intro: 'இந்த விருப்பங்கள் அனைத்து தொகுதிகளிலும் பொருந்தும்.', recent: 'சமீபத்திய',
      operations: 'செயல்பாடுகள்', business_partners: 'வணிக கூட்டாளர்கள்', finance: 'நிதி',
      insight_system: 'நுண்ணறிவு மற்றும் கணினி',
      sign_in: 'உள்நுழைக', sign_in_desc: 'உங்கள் Universal ERP பணிமனையில் உள்நுழையவும்',
      sign_up: 'உங்கள் பணிமனையை உருவாக்கவும்', sign_up_desc: 'புதிய நிறுவனம் மற்றும் நிர்வாக கணக்கை அமைக்கவும்',
      create_account: 'கணக்கை உருவாக்கு', email: 'மின்னஞ்சல்', password: 'கடவுச்சொல்',
      confirm_password: 'கடவுச்சொல்லை உறுதிப்படுத்து', full_name: 'முழு பெயர்', organization_name: 'நிறுவனத்தின் பெயர்',
      forgot_password: 'கடவுச்சொல் மறந்துவிட்டதா?', remember_me: 'என்னை நினைவில் கொள்',
      dont_have_account: 'கணக்கு இல்லையா?', already_have_account: 'ஏற்கனவே கணக்கு உள்ளதா?',
      invalid_credentials: 'தவறான மின்னஞ்சல் அல்லது கடவுச்சொல்.', weak_password: 'கடவுச்சொல் குறைந்தது 8 எழுத்துகள் இருக்க வேண்டும்.',
      passwords_dont_match: 'கடவுச்சொற்கள் பொருந்தவில்லை.', agree_terms_prefix: 'நான் ஒப்புக்கொள்கிறேன்',
      terms_of_service: 'சேவை விதிமுறைகள்', and_word: 'மற்றும்', privacy_policy: 'தனியுரிமைக் கொள்கை',
      sessions_title: 'செயலில் உள்ள அமர்வுகள்', sessions_intro: 'உங்கள் கணக்கில் தற்போது உள்நுழைந்துள்ள சாதனங்கள்.',
      this_device: 'இந்த சாதனம்', revoke: 'ரத்து செய்', revoke_all_others: 'மற்ற அனைத்து அமர்வுகளையும் ரத்து செய்',
      last_active: 'கடைசியாக செயலில் இருந்தது', signed_in_as: 'இப்படி உள்நுழைந்துள்ளீர்கள்',
      roles_title: 'பங்குகள் மற்றும் அனுமதிகள்',
      roles_intro: 'பங்குகள் அனுமதிகளைத் தொகுக்கின்றன. ஒரு பயனருக்கு ஒரு பங்கை ஒதுக்கி அது அனுமதிக்கும் அனைத்தையும் வழங்கவும்.',
      new_role: 'புதிய பங்கு', role_col: 'பங்கு', description_col: 'விளக்கம்', members_col: 'உறுப்பினர்கள்', type_col: 'வகை',
      system_role: 'கணினி', custom_role: 'தனிப்பயன்', permissions: 'அனுமதிகள்', module_col: 'தொகுதி',
      scope_tenant: 'நோக்கு: முழு நிறுவனம்',
      system_role_note: 'இது உள்ளமைந்த கணினி பங்கு. அதன் அனுமதிகள் நிலையானவை, அதை நீக்க முடியாது. திருத்தக்கூடிய நகலை உருவாக்க அதை நகலெடுக்கவும்.',
      perm_view: 'பார்', perm_create: 'உருவாக்கு', perm_edit: 'திருத்து', perm_delete: 'நீக்கு', perm_approve: 'ஒப்புதல்',
      perm_legend: 'பார்வை பயனர் பதிவுகளைத் திறக்க அனுமதிக்கிறது. உருவாக்கு, திருத்து, நீக்கு அவற்றை மாற்றுகின்றன. ஒப்புதல் ஆர்டர்கள் மற்றும் விலைப்பட்டியல்கள் போன்ற ஆவணங்களை உறுதிப்படுத்துகிறது.',
      role_info: 'பங்கு தகவல்', role_name_label: 'பங்கின் பெயர்', scope_label: 'நோக்கு',
      scope_hint: 'கிளை மற்றும் கடை நிலை பங்குகள் பிற்கால வெளியீட்டிற்குத் திட்டமிடப்பட்டுள்ளன.',
      members: 'உறுப்பினர்கள்', members_empty: 'இந்தப் பங்கு இன்னும் எந்தப் பயனருக்கும் இல்லை.', add_member: 'உறுப்பினரைச் சேர்',
      administration: 'நிர்வாகம்',
      audit_intro: 'அடையாளம், பாதுகாப்பு மற்றும் வணிக தரவு முழுவதும் பதிவு செய்யப்பட்ட ஒவ்வொரு செயலும் - யார் என்ன செய்தார்கள், எப்போது, என்ன மாறியது.',
      search_audit_logs: 'பயனர், பொருள், அல்லது செயல் மூலம் தேடு...', entity_col: 'பொருள்', actor_col: 'பயனர்',
      action_col: 'செயல்', timestamp_col: 'நேரம்', organization_col: 'அமைப்பு', diff_col: 'விவரங்கள்',
      all_entities: 'அனைத்து பொருட்களும்', all_actions: 'அனைத்து செயல்களும்', all_actors: 'அனைத்து பயனர்களும்',
      date_from: 'இருந்து', date_to: 'வரை', view_diff: 'விவரங்களைப் பார்', diff_title: 'மாற்ற விவரங்கள்',
      before_label: 'முன்பு', after_label: 'பின்பு', close: 'மூடு',
      no_audit_results: 'உங்கள் வடிகட்டிகளுக்குப் பொருந்தும் தணிக்கை பதிவுகள் இல்லை.', unknown_actor: 'தெரியாதது'
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
