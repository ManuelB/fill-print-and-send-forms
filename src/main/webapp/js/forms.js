
//
// Basic AcroForms input controls rendering
//

'use strict';

var formFields = {};

if(typeof PDFJS === "undefined") {
	var PDFJS = {};
}

// global pageViewportArray
PDFJS.pageViewports = {};

function setupForm(div, content, viewport) {
  function bindInputItem(input, item) {
    if (input.name in formFields) {
      var value = formFields[input.name];
      if (input.type == 'checkbox') {
        input.checked = value;
      } else if (!input.type || input.type == 'text') {
        input.value = value;
      }
    }
    input.onchange = function pageViewSetupInputOnBlur() {
      if (input.type == 'checkbox') {
        formFields[input.name] = input.checked;
      } else if (!input.type || input.type == 'text') {
        formFields[input.name] = input.value;
      }
    };
  }
  function createElementWithStyle(tagName, item) {
    var element = document.createElement(tagName);
    var rect = PDFJS.Util.normalizeRect(
      viewport.convertToViewportRectangle(item.rect));
    element.style.left = Math.floor(rect[0]) + 'px';
    element.style.top = Math.floor(rect[1]) + 'px';
    element.style.width = Math.ceil(rect[2] - rect[0]) + 'px';
    element.style.height = Math.ceil(rect[3] - rect[1]) + 'px';
    return element;
  }
  function assignFontStyle(element, item) {
    var fontStyles = '';
    if ('fontSize' in item) {
      fontStyles += 'font-size: ' + Math.round(item.fontSize *
                                               viewport.fontScale) + 'px;';
    }
    switch (item.textAlignment) {
      case 0:
        fontStyles += 'text-align: left;';
        break;
      case 1:
        fontStyles += 'text-align: center;';
        break;
      case 2:
        fontStyles += 'text-align: right;';
        break;
    }
    element.setAttribute('style', element.getAttribute('style') + fontStyles);
  }

  content.getAnnotations().then(function(items) {
    for (var i = 0; i < items.length; i++) {
      var item = items[i];
      switch (item.subtype) {
        case 'Widget':
          if (item.fieldType != 'Tx' && item.fieldType != 'Btn' &&
              item.fieldType != 'Ch') {
            break;
          }
          var inputDiv = createElementWithStyle('div', item);
          inputDiv.className = 'inputHint';
          div.appendChild(inputDiv);
          
          var input;
          if(item.fullName === "signature-drawing.signature-drawing") {
        	  $(inputDiv).addClass("signature");
        	  $(inputDiv).jSignature({
        		  'UndoButton' : true,
        		  'width' : $(inputDiv).width(),
        		  'height' : $(inputDiv).height()
        	  });
        	  
        	  input = document.createElement("input");
        	  input.type = "hidden";
        	  input.name = item.fullName;
        	  
        	  $(inputDiv).bind('change', function(e){
        		  /* 'e.target' will refer to div with "#signature" */
        		  // We had bigger problems with batik
        		  // var datapair = $(e.target).jSignature("getData","svg");
        		  var datapair = $(e.target).jSignature("getData","image");
        		  input.value = datapair[1];
        	  });
        	  div.appendChild(input);
        	  
          } else {
        	  if (item.fieldType == 'Tx') {
        		  input = createElementWithStyle('input', item);
        	  }
        	  if (item.fieldType == 'Btn') {
        		  input = createElementWithStyle('input', item);
        		  if (item.flags & 32768) {
        			  input.type = 'radio';
        			  // radio button is not supported
        		  } else if (item.flags & 65536) {
        			  input.type = 'button';
        			  // pushbutton is not supported
        		  } else {
        			  input.type = 'checkbox';
        			  input.style.visibility = 'visible';
        		  }
        	  }
        	  if (item.fieldType == 'Ch') {
        		  input = createElementWithStyle('select', item);
        		  // select box is not supported
        	  }
        	  input.className = 'inputControl';
        	  input.name = item.fullName;
        	  input.title = item.alternativeText;
        	  assignFontStyle(input, item);
        	  bindInputItem(input, item);
        	  div.appendChild(input);
          }
          break;
      }
    }
  });
}

function renderPage(div, pdf, pageNumber, callback) {
  pdf.getPage(pageNumber).then(function(page) {
    var scale = 1.5;
    var viewport = page.getViewport(scale);
    
    PDFJS.pageViewports[pageNumber] = viewport;

    var pageDisplayWidth = viewport.width;
    var pageDisplayHeight = viewport.height;

    var pageDivHolder = document.createElement('div');
    pageDivHolder.className = 'pdfpage';
    pageDivHolder.title = pageNumber;
    pageDivHolder.style.width = pageDisplayWidth + 'px';
    pageDivHolder.style.height = pageDisplayHeight + 'px';
    div.appendChild(pageDivHolder);

    // Prepare canvas using PDF page dimensions
    var canvas = document.createElement('canvas');
    var context = canvas.getContext('2d');
    canvas.width = pageDisplayWidth;
    canvas.height = pageDisplayHeight;
    pageDivHolder.appendChild(canvas);

    // Render PDF page into canvas context
    var renderContext = {
      canvasContext: context,
      viewport: viewport
    };
    page.render(renderContext).promise.then(callback);

    // Prepare and populate form elements layer
    var formDiv = document.createElement('div');
    pageDivHolder.appendChild(formDiv);

    setupForm(formDiv, page, viewport);
  });
}

// In production, the bundled pdf.js shall be used instead of RequireJS.
require.config({paths: {'pdfjs': 'src'}});
require(['pdfjs/display/api'], function (api) {
  // In production, change this to point to the built `pdf.worker.js` file.
  PDFJS.workerSrc = 'src/worker_loader.js';

  var formFileName = $('#form-file-name').val();
  if(formFileName) {
	  //Specify the PDF with AcroForm here
	  var pdfWithFormsPath = "documents/"+formFileName;
	  
	  // Fetch the PDF document from the URL using promises.
	  api.getDocument(pdfWithFormsPath).then(function getPdfForm(pdf) {
		  
		  pdf.getMetadata().then(function (infos) {
			  try {
				  $('#receiver-for-letter').val(infos.info.Author);
			  } catch(e) {
				  console.error(e);
			  }
		  });
		  
		  // Rendering all pages starting from first
		  var viewer = document.getElementById('viewer');
		  var pageNumber = 1;
		  renderPage(viewer, pdf, pageNumber++, function pageRenderingComplete() {
			  if (pageNumber > pdf.numPages) {
				  return; // All pages rendered
			  }
			  // Continue rendering of the next page
			  renderPage(viewer, pdf, pageNumber++, pageRenderingComplete);
		  });
	  });
  }
});
