/* Copyright 2014 Mozilla Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* globals PDFJS */

'use strict';

(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    define('pdfjs/display/annotation_layer', ['exports', 'pdfjs/shared/util',
      'pdfjs/display/dom_utils'], factory);
  } else if (typeof exports !== 'undefined') {
    factory(exports, require('../shared/util.js'), require('./dom_utils.js'));
  } else {
    factory((root.pdfjsDisplayAnnotationLayer = {}), root.pdfjsSharedUtil,
      root.pdfjsDisplayDOMUtils);
  }
}(this, function (exports, sharedUtil, displayDOMUtils) {

var AnnotationBorderStyleType = sharedUtil.AnnotationBorderStyleType;
var AnnotationType = sharedUtil.AnnotationType;
var Util = sharedUtil.Util;
var isExternalLinkTargetSet = sharedUtil.isExternalLinkTargetSet;
var LinkTargetStringMap = sharedUtil.LinkTargetStringMap;
var removeNullCharacters = sharedUtil.removeNullCharacters;
var warn = sharedUtil.warn;
var CustomStyle = displayDOMUtils.CustomStyle;

var READONLY_BIT = 1;
var MULTILINE_BIT = 13;

/**
 * @typedef {Object} AnnotationElementParameters
 * @property {Object} data
 * @property {HTMLDivElement} layer
 * @property {PDFPage} page
 * @property {PageViewport} viewport
 * @property {IPDFLinkService} linkService
 */

/**
 * @class
 * @alias AnnotationElementFactory
 */
function AnnotationElementFactory() {}
AnnotationElementFactory.prototype =
    /** @lends AnnotationElementFactory.prototype */ {
  /**
   * @param {AnnotationElementParameters} parameters
   * @returns {AnnotationElement}
   */
  create: function AnnotationElementFactory_create(parameters) {
    var subtype = parameters.data.annotationType;

    switch (subtype) {
      case AnnotationType.LINK:
        return new LinkAnnotationElement(parameters);

      case AnnotationType.TEXT:
        return new TextAnnotationElement(parameters);

      case AnnotationType.WIDGET:
        switch (parameters.data.fieldType) {
          case 'Tx':
            return new TextWidgetAnnotationElement(parameters);
          default:
            warn('Unimplemented Widget annotation type: ' +
                                             parameters.data.fieldType);
            // Fallback to default one, which does not render anything
            return new WidgetAnnotationElement(parameters);
        }
        break; // make linter happy

      case AnnotationType.POPUP:
        return new PopupAnnotationElement(parameters);

      case AnnotationType.HIGHLIGHT:
        return new HighlightAnnotationElement(parameters);

      case AnnotationType.UNDERLINE:
        return new UnderlineAnnotationElement(parameters);

      case AnnotationType.SQUIGGLY:
        return new SquigglyAnnotationElement(parameters);

      case AnnotationType.STRIKEOUT:
        return new StrikeOutAnnotationElement(parameters);

      default:
        throw new Error('Unimplemented annotation type "' + subtype + '"');
    }
  }
};

/**
 * @class
 * @alias AnnotationElement
 */
var AnnotationElement = (function AnnotationElementClosure() {
  function AnnotationElement(parameters) {
    this.data = parameters.data;
    this.layer = parameters.layer;
    this.page = parameters.page;
    this.viewport = parameters.viewport;
    this.linkService = parameters.linkService;

    this.container = this._createContainer();
  }

  AnnotationElement.prototype = /** @lends AnnotationElement.prototype */ {
    /**
     * Create an empty container for the annotation's HTML element.
     *
     * @private
     * @memberof AnnotationElement
     * @returns {HTMLSectionElement}
     */
    _createContainer: function AnnotationElement_createContainer() {
      var data = this.data, page = this.page, viewport = this.viewport;
      var container = document.createElement('section');
      var width = data.rect[2] - data.rect[0];
      var height = data.rect[3] - data.rect[1];

      container.setAttribute('data-annotation-id', data.id);

      // Do *not* modify `data.rect`, since that will corrupt the annotation
      // position on subsequent calls to `_createContainer` (see issue 6804).
      var rect = Util.normalizeRect([
        data.rect[0],
        page.view[3] - data.rect[1] + page.view[1],
        data.rect[2],
        page.view[3] - data.rect[3] + page.view[1]
      ]);

      CustomStyle.setProp('transform', container,
                          'matrix(' + viewport.transform.join(',') + ')');
      CustomStyle.setProp('transformOrigin', container,
                          -rect[0] + 'px ' + -rect[1] + 'px');

      if (data.borderStyle.width > 0) {
        container.style.borderWidth = data.borderStyle.width + 'px';
        if (data.borderStyle.style !== AnnotationBorderStyleType.UNDERLINE) {
          // Underline styles only have a bottom border, so we do not need
          // to adjust for all borders. This yields a similar result as
          // Adobe Acrobat/Reader.
          width = width - 2 * data.borderStyle.width;
          height = height - 2 * data.borderStyle.width;
        }

        var horizontalRadius = data.borderStyle.horizontalCornerRadius;
        var verticalRadius = data.borderStyle.verticalCornerRadius;
        if (horizontalRadius > 0 || verticalRadius > 0) {
          var radius = horizontalRadius + 'px / ' + verticalRadius + 'px';
          CustomStyle.setProp('borderRadius', container, radius);
        }

        switch (data.borderStyle.style) {
          case AnnotationBorderStyleType.SOLID:
            container.style.borderStyle = 'solid';
            break;

          case AnnotationBorderStyleType.DASHED:
            container.style.borderStyle = 'dashed';
            break;

          case AnnotationBorderStyleType.BEVELED:
            warn('Unimplemented border style: beveled');
            break;

          case AnnotationBorderStyleType.INSET:
            warn('Unimplemented border style: inset');
            break;

          case AnnotationBorderStyleType.UNDERLINE:
            container.style.borderBottomStyle = 'solid';
            break;

          default:
            break;
        }

        if (data.color) {
          container.style.borderColor =
            Util.makeCssRgb(data.color[0] | 0,
                            data.color[1] | 0,
                            data.color[2] | 0);
        } else {
          // Transparent (invisible) border, so do not draw it at all.
          container.style.borderWidth = 0;
        }
      }

      container.style.left = rect[0] + 'px';
      container.style.top = rect[1] + 'px';

      container.style.width = width + 'px';
      container.style.height = height + 'px';

      return container;
    },

    /**
     * Render the annotation's HTML element in the empty container.
     *
     * @public
     * @memberof AnnotationElement
     */
    render: function AnnotationElement_render() {
      throw new Error('Abstract method AnnotationElement.render called');
    }
  };

  return AnnotationElement;
})();

/**
 * @class
 * @alias LinkAnnotationElement
 */
var LinkAnnotationElement = (function LinkAnnotationElementClosure() {
  function LinkAnnotationElement(parameters) {
    AnnotationElement.call(this, parameters);
  }

  Util.inherit(LinkAnnotationElement, AnnotationElement, {
    /**
     * Render the link annotation's HTML element in the empty container.
     *
     * @public
     * @memberof LinkAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function LinkAnnotationElement_render() {
      this.container.className = 'linkAnnotation';

      var link = document.createElement('a');
      link.href = link.title = (this.data.url ?
                                removeNullCharacters(this.data.url) : '');

      if (this.data.url && isExternalLinkTargetSet()) {
        link.target = LinkTargetStringMap[PDFJS.externalLinkTarget];
      }

      // Strip referrer from the URL.
      if (this.data.url) {
        link.rel = PDFJS.externalLinkRel;
      }

      if (!this.data.url) {
        if (this.data.action) {
          this._bindNamedAction(link, this.data.action);
        } else {
          this._bindLink(link, ('dest' in this.data) ? this.data.dest : null);
        }
      }

      this.container.appendChild(link);
      return this.container;
    },

    /**
     * Bind internal links to the link element.
     *
     * @private
     * @param {Object} link
     * @param {Object} destination
     * @memberof LinkAnnotationElement
     */
    _bindLink: function LinkAnnotationElement_bindLink(link, destination) {
      var self = this;

      link.href = this.linkService.getDestinationHash(destination);
      link.onclick = function() {
        if (destination) {
          self.linkService.navigateTo(destination);
        }
        return false;
      };
      if (destination) {
        link.className = 'internalLink';
      }
    },

    /**
     * Bind named actions to the link element.
     *
     * @private
     * @param {Object} link
     * @param {Object} action
     * @memberof LinkAnnotationElement
     */
    _bindNamedAction:
        function LinkAnnotationElement_bindNamedAction(link, action) {
      var self = this;

      link.href = this.linkService.getAnchorUrl('');
      link.onclick = function() {
        self.linkService.executeNamedAction(action);
        return false;
      };
      link.className = 'internalLink';
    }
  });

  return LinkAnnotationElement;
})();

/**
 * @class
 * @alias TextAnnotationElement
 */
var TextAnnotationElement = (function TextAnnotationElementClosure() {
  function TextAnnotationElement(parameters) {
    AnnotationElement.call(this, parameters);
  }

  Util.inherit(TextAnnotationElement, AnnotationElement, {
    /**
     * Render the text annotation's HTML element in the empty container.
     *
     * @public
     * @memberof TextAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function TextAnnotationElement_render() {
      this.container.className = 'textAnnotation';

      var image = document.createElement('img');
      image.style.height = this.container.style.height;
      image.style.width = this.container.style.width;
      image.src = PDFJS.imageResourcesPath + 'annotation-' +
        this.data.name.toLowerCase() + '.svg';
      image.alt = '[{{type}} Annotation]';
      image.dataset.l10nId = 'text_annotation_type';
      image.dataset.l10nArgs = JSON.stringify({type: this.data.name});

      if (!this.data.hasPopup) {
        var popupElement = new PopupElement({
          container: this.container,
          trigger: image,
          color: this.data.color,
          title: this.data.title,
          contents: this.data.contents,
          hideWrapper: true
        });
        var popup = popupElement.render();

        // Position the popup next to the Text annotation's container.
        popup.style.left = image.style.width;

        this.container.appendChild(popup);
      }

      this.container.appendChild(image);
      return this.container;
    }
  });

  return TextAnnotationElement;
})();

/**
 * @class
 * @alias WidgetAnnotationElement
 */
var WidgetAnnotationElement = (function WidgetAnnotationElementClosure() {
  function WidgetAnnotationElement(parameters) {
    AnnotationElement.call(this, parameters);
  }

  Util.inherit(WidgetAnnotationElement, AnnotationElement, {
    /**
     * Render the widget annotation's HTML element in the empty container.
     *
     * @public
     * @memberof WidgetAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function WidgetAnnotationElement_render() {
      return this.container;
    },

    /**
     *
     * @protected
     * @memberof WidgetAnnotationElement
     * @returns {boolean}
     */
    isFlagSet: function WidgetAnnotationElement_isFlagSet(pos) {
      // Note: pos is 1 based index
      return !!(this.data.fieldFlags & (1 << (pos - 1)));
    }
  });

  return WidgetAnnotationElement;
})();

/**
 * @class
 * @alias TextWidgetAnnotationElement
 */
var TextWidgetAnnotationElement =
    (function TextWidgetAnnotationElementClosure() {
  function TextWidgetAnnotationElement(parameters) {
    WidgetAnnotationElement.call(this, parameters);
  }

  Util.inherit(TextWidgetAnnotationElement, WidgetAnnotationElement, {
    /**
     *
     * @public
     * @memberof TextWidgetAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function TextWidgetAnnotationElement_render() {
      var container = WidgetAnnotationElement.prototype.render.call(this);

      var isReadonly = this.isFlagSet(READONLY_BIT);
      var isMultiline = this.isFlagSet(MULTILINE_BIT);

      var content;

      if (isMultiline) {
        content = document.createElement('textarea');
      } else {
        content = document.createElement('input');
        content.type = 'text';
      }

      content.disabled = isReadonly;
      content.value = this.data.fieldValue;
      var textAlignment = this.data.textAlignment;
      content.style.textAlign = ['left', 'center', 'right'][textAlignment];
      if (this.data.maxLen !== null) {
        content.maxLength = this.data.maxLen;
      }

      var font = (this.data.fontRefName ?
        this.page.commonObjs.getData(this.data.fontRefName) : null);
      this._setTextStyle(content, font);

      if (!isMultiline && !('fontSize' in this.data)) {
        // hack to guess font size base on content hight
        // so it display fine on small text field
        // this need to be removed when we can apply defaultAppearance
        var height = this.data.rect[3] - this.data.rect[1];
        if (height < 15) {
            content.style.fontSize = (height - 1) + 'px';
        }
      }

      container.appendChild(content);

      container.className = 'widgetAnnotation';
      return container;
    },

    /**
     * Apply text styles to the text in the element.
     *
     * @private
     * @param {HTMLDivElement} element
     * @param {Object} font
     * @memberof TextWidgetAnnotationElement
     */
    _setTextStyle:
        function TextWidgetAnnotationElement_setTextStyle(element, font) {
      // TODO: This duplicates some of the logic in CanvasGraphics.setFont().
      var style = element.style;
      if ('fontSize' in this.data) {
        style.fontSize = this.data.fontSize + 'px';
      }
      if ('fontDirection' in this.data) {
        style.direction = (this.data.fontDirection < 0 ? 'rtl': 'ltr');
      }

      if (!font) {
        return;
      }

      style.fontWeight = (font.black ?
        (font.bold ? '900' : 'bold') :
        (font.bold ? 'bold' : 'normal'));
      style.fontStyle = (font.italic ? 'italic' : 'normal');

      // Use a reasonable default font if the font doesn't specify a fallback.
      var fontFamily = font.loadedName ? '"' + font.loadedName + '", ' : '';
      var fallbackName = font.fallbackName || 'Helvetica, sans-serif';
      style.fontFamily = fontFamily + fallbackName;
    }
  });

  return TextWidgetAnnotationElement;
})();

/**
 * @class
 * @alias PopupAnnotationElement
 */
var PopupAnnotationElement = (function PopupAnnotationElementClosure() {
  function PopupAnnotationElement(parameters) {
    AnnotationElement.call(this, parameters);
  }

  Util.inherit(PopupAnnotationElement, AnnotationElement, {
    /**
     * Render the popup annotation's HTML element in the empty container.
     *
     * @public
     * @memberof PopupAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function PopupAnnotationElement_render() {
      this.container.className = 'popupAnnotation';

      var selector = '[data-annotation-id="' + this.data.parentId + '"]';
      var parentElement = this.layer.querySelector(selector);
      if (!parentElement) {
        return this.container;
      }

      var popup = new PopupElement({
        container: this.container,
        trigger: parentElement,
        color: this.data.color,
        title: this.data.title,
        contents: this.data.contents
      });

      // Position the popup next to the parent annotation's container.
      // PDF viewers ignore a popup annotation's rectangle.
      var parentLeft = parseFloat(parentElement.style.left);
      var parentWidth = parseFloat(parentElement.style.width);
      CustomStyle.setProp('transformOrigin', this.container,
                          -(parentLeft + parentWidth) + 'px -' +
                          parentElement.style.top);
      this.container.style.left = (parentLeft + parentWidth) + 'px';

      this.container.appendChild(popup.render());
      return this.container;
    }
  });

  return PopupAnnotationElement;
})();

/**
 * @class
 * @alias PopupElement
 */
var PopupElement = (function PopupElementClosure() {
  var BACKGROUND_ENLIGHT = 0.7;

  function PopupElement(parameters) {
    this.container = parameters.container;
    this.trigger = parameters.trigger;
    this.color = parameters.color;
    this.title = parameters.title;
    this.contents = parameters.contents;
    this.hideWrapper = parameters.hideWrapper || false;

    this.pinned = false;
  }

  PopupElement.prototype = /** @lends PopupElement.prototype */ {
    /**
     * Render the popup's HTML element.
     *
     * @public
     * @memberof PopupElement
     * @returns {HTMLSectionElement}
     */
    render: function PopupElement_render() {
      var wrapper = document.createElement('div');
      wrapper.className = 'popupWrapper';

      // For Popup annotations we hide the entire section because it contains
      // only the popup. However, for Text annotations without a separate Popup
      // annotation, we cannot hide the entire container as the image would
      // disappear too. In that special case, hiding the wrapper suffices.
      this.hideElement = (this.hideWrapper ? wrapper : this.container);
      this.hideElement.setAttribute('hidden', true);

      var popup = document.createElement('div');
      popup.className = 'popup';

      var color = this.color;
      if (color) {
        // Enlighten the color.
        var r = BACKGROUND_ENLIGHT * (255 - color[0]) + color[0];
        var g = BACKGROUND_ENLIGHT * (255 - color[1]) + color[1];
        var b = BACKGROUND_ENLIGHT * (255 - color[2]) + color[2];
        popup.style.backgroundColor = Util.makeCssRgb(r | 0, g | 0, b | 0);
      }

      var contents = this._formatContents(this.contents);
      var title = document.createElement('h1');
      title.textContent = this.title;

      // Attach the event listeners to the trigger element.
      this.trigger.addEventListener('click', this._toggle.bind(this));
      this.trigger.addEventListener('mouseover', this._show.bind(this, false));
      this.trigger.addEventListener('mouseout', this._hide.bind(this, false));
      popup.addEventListener('click', this._hide.bind(this, true));

      popup.appendChild(title);
      popup.appendChild(contents);
      wrapper.appendChild(popup);
      return wrapper;
    },

    /**
     * Format the contents of the popup by adding newlines where necessary.
     *
     * @private
     * @param {string} contents
     * @memberof PopupElement
     * @returns {HTMLParagraphElement}
     */
    _formatContents: function PopupElement_formatContents(contents) {
      var p = document.createElement('p');
      var lines = contents.split(/(?:\r\n?|\n)/);
      for (var i = 0, ii = lines.length; i < ii; ++i) {
        var line = lines[i];
        p.appendChild(document.createTextNode(line));
        if (i < (ii - 1)) {
          p.appendChild(document.createElement('br'));
        }
      }
      return p;
    },

    /**
     * Toggle the visibility of the popup.
     *
     * @private
     * @memberof PopupElement
     */
    _toggle: function PopupElement_toggle() {
      if (this.pinned) {
        this._hide(true);
      } else {
        this._show(true);
      }
    },

    /**
     * Show the popup.
     *
     * @private
     * @param {boolean} pin
     * @memberof PopupElement
     */
    _show: function PopupElement_show(pin) {
      if (pin) {
        this.pinned = true;
      }
      if (this.hideElement.hasAttribute('hidden')) {
        this.hideElement.removeAttribute('hidden');
        this.container.style.zIndex += 1;
      }
    },

    /**
     * Hide the popup.
     *
     * @private
     * @param {boolean} unpin
     * @memberof PopupElement
     */
    _hide: function PopupElement_hide(unpin) {
      if (unpin) {
        this.pinned = false;
      }
      if (!this.hideElement.hasAttribute('hidden') && !this.pinned) {
        this.hideElement.setAttribute('hidden', true);
        this.container.style.zIndex -= 1;
      }
    }
  };

  return PopupElement;
})();

/**
 * @class
 * @alias HighlightAnnotationElement
 */
var HighlightAnnotationElement = (
    function HighlightAnnotationElementClosure() {
  function HighlightAnnotationElement(parameters) {
    AnnotationElement.call(this, parameters);
  }

  Util.inherit(HighlightAnnotationElement, AnnotationElement, {
    /**
     * Render the highlight annotation's HTML element in the empty container.
     *
     * @public
     * @memberof HighlightAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function HighlightAnnotationElement_render() {
      this.container.className = 'highlightAnnotation';
      return this.container;
    }
  });

  return HighlightAnnotationElement;
})();

/**
 * @class
 * @alias UnderlineAnnotationElement
 */
var UnderlineAnnotationElement = (
    function UnderlineAnnotationElementClosure() {
  function UnderlineAnnotationElement(parameters) {
    AnnotationElement.call(this, parameters);
  }

  Util.inherit(UnderlineAnnotationElement, AnnotationElement, {
    /**
     * Render the underline annotation's HTML element in the empty container.
     *
     * @public
     * @memberof UnderlineAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function UnderlineAnnotationElement_render() {
      this.container.className = 'underlineAnnotation';
      return this.container;
    }
  });

  return UnderlineAnnotationElement;
})();

/**
 * @class
 * @alias SquigglyAnnotationElement
 */
var SquigglyAnnotationElement = (function SquigglyAnnotationElementClosure() {
  function SquigglyAnnotationElement(parameters) {
    AnnotationElement.call(this, parameters);
  }

  Util.inherit(SquigglyAnnotationElement, AnnotationElement, {
    /**
     * Render the squiggly annotation's HTML element in the empty container.
     *
     * @public
     * @memberof SquigglyAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function SquigglyAnnotationElement_render() {
      this.container.className = 'squigglyAnnotation';
      return this.container;
    }
  });

  return SquigglyAnnotationElement;
})();

/**
 * @class
 * @alias StrikeOutAnnotationElement
 */
var StrikeOutAnnotationElement = (
    function StrikeOutAnnotationElementClosure() {
  function StrikeOutAnnotationElement(parameters) {
    AnnotationElement.call(this, parameters);
  }

  Util.inherit(StrikeOutAnnotationElement, AnnotationElement, {
    /**
     * Render the strikeout annotation's HTML element in the empty container.
     *
     * @public
     * @memberof StrikeOutAnnotationElement
     * @returns {HTMLSectionElement}
     */
    render: function StrikeOutAnnotationElement_render() {
      this.container.className = 'strikeoutAnnotation';
      return this.container;
    }
  });

  return StrikeOutAnnotationElement;
})();

/**
 * @typedef {Object} AnnotationLayerParameters
 * @property {PageViewport} viewport
 * @property {HTMLDivElement} div
 * @property {Array} annotations
 * @property {PDFPage} page
 * @property {IPDFLinkService} linkService
 */

/**
 * @class
 * @alias AnnotationLayer
 */
var AnnotationLayer = (function AnnotationLayerClosure() {
  return {
    /**
     * Render a new annotation layer with all annotation elements.
     *
     * @public
     * @param {AnnotationLayerParameters} parameters
     * @memberof AnnotationLayer
     */
    render: function AnnotationLayer_render(parameters) {
      var annotationElementFactory = new AnnotationElementFactory();

      for (var i = 0, ii = parameters.annotations.length; i < ii; i++) {
        var data = parameters.annotations[i];
        if (!data || !data.hasHtml) {
          continue;
        }

        var properties = {
          data: data,
          layer: parameters.div,
          page: parameters.page,
          viewport: parameters.viewport,
          linkService: parameters.linkService
        };
        var element = annotationElementFactory.create(properties);
        parameters.div.appendChild(element.render());
      }
    },

    /**
     * Update the annotation elements on existing annotation layer.
     *
     * @public
     * @param {AnnotationLayerParameters} parameters
     * @memberof AnnotationLayer
     */
    update: function AnnotationLayer_update(parameters) {
      for (var i = 0, ii = parameters.annotations.length; i < ii; i++) {
        var data = parameters.annotations[i];
        var element = parameters.div.querySelector(
          '[data-annotation-id="' + data.id + '"]');
        if (element) {
          CustomStyle.setProp('transform', element,
            'matrix(' + parameters.viewport.transform.join(',') + ')');
        }
      }
      parameters.div.removeAttribute('hidden');
    }
  };
})();

PDFJS.AnnotationLayer = AnnotationLayer;

exports.AnnotationLayer = AnnotationLayer;
}));
