<html>
	<head>
	    <style>
    @page {
        size: a4 landscape;
        margin: 50px;
        @bottom-right {
            font-family: "FreeSans";
            font-size: 10px;
            content: '${pageLabel} '+counter(page)+' ${ofLabel} '+counter(pages);
        }
    }
    page-before {
      display: block;
      /* Create a page break before this element. */
      page-break-before: always;
    }
    body {
        margin: 0px;
        font-family: "FreeSans";
        font-size: 14px;
        line-height: 1.4;
    }
    table {
        border-spacing: 0px;
    }
    td, th {
        font-family: "FreeSans";
        font-size: 14px;
        line-height: 1.4;
    }
    .title {
        font-size: 30px;
        margin-bottom: 30px;
        background-color: #ededed;
        padding: 10px;
        border-radius: 5px;
        border: 1px solid #000000;
        page-break-inside: avoid;
    }
    .page-break-avoid {
        page-break-inside: avoid;
    }
    .sub-title {
        font-size: 18px;
        margin-bottom: 30px;
        background-color: #ededed;
        padding: 10px;
        border-radius: 5px;
        border: 1px solid #000000;
        position: relative;
        page-break-inside: avoid;
    }
    .sub-title-text {
        display: inline-block;
        margin-right: 30px;
        width: 90%;
    }
    .sub-title-link {
        margin-top: 2px;
        font-size: 14px;
        text-align: right;
        float: right;
    }
    .section-title {
        border-bottom: 1px solid #000000;
        padding-bottom: 5px;
        margin-bottom: 10px;
        font-size: 18px;
        padding-left: 15px;
        page-break-inside: avoid;
    }
    .section {
        margin-top: 30px;
    }
    .section.no-margin {
        margin-top: 0px;
    }
    .row {
        display: block;
        padding: 2px 0px 2px 0px;
    }
    .column {
        display: inline-block;
        vertical-align: top;
    }
    .column.left {
        width: 39%;
    }
    .column.right {
        width: 60%;
    }
    .value {
        display: inline-block;
        vertical-align: top;
    }
    .value-inline {
        display: inline;
        vertical-align: top;
    }
    .label {
        font-weight: bold;
        display: inline-block;
        vertical-align: top;
    }
    td.cell-label {
        font-weight: bold;
        padding-left: 10px;
        padding-right: 10px;
        text-align: right;
        min-width: 120px;
    }
    td.cell-label, td.cell-value {
        vertical-align: top;
        padding: 4px;
    }
    .result {
        font-size: 90%;
        padding: 0.3em 0.6em 0.3em 0.6em;
        font-weight: bold;
        color: #fff;
        text-align: center;
        white-space: nowrap;
        vertical-align: baseline;
        border-radius: 4px;
    }
    .icon {
        display: inline;
        padding-top: 2px;
        padding-bottom: 2px;
    }
    .icon img {
        width: 16px;
        margin-top: -4px;
    }
    .separator {
        margin-top: 10px;
        margin-left: 10px;
        margin-right: 10px;
        padding-top: 10px;
        border-top: 1px solid #c4c4c4;
    }
    .background-SUCCESS {
        background-color: #5cb85c;
    }
    .background-FAILURE {
        background-color: #c9302c;
    }
    .background-WARNING {
        background-color: #f0ad4e;
    }
    .background-UNDEFINED {
        background-color: #7c7c7c;
    }
    .background-strong-error {
        background-color: #c9302c;
    }
    .background-strong-warning {
        background-color: #f0ad4e;
    }
    .background-strong-info {
        background-color: #3D8BE9;
    }
    .background-error {
        background-color: #f2dede;
    }
    .background-warning {
        background-color: #fcf8e3;
    }
    .background-info {
        background-color: #ededed;
    }
    .background-normal {
        background: #ededed;
    }
    .border-normal {
        border: 1px solid #000000;
    }
    .border-error {
        border: 1px solid #c9302c;
    }
    .border-warning {
        border: 1px solid #f0ad4e;
    }
    .border-info {
        border: 1px solid #3D8BE9;
    }
    .report-item {
        margin-top: 10px;
        margin-bottom: 10px;
        border-radius: 4px;
        page-break-inside: avoid;
    }
    .step-report .section.overview td.cell-label {
        min-width: inherit;
        padding-left: 20px;
    }
    .step-report .details .metadata .row .label {
        margin-right: 10px;
    }
    .report-item-container {
        margin-left:5px;
        border-bottom-right-radius: 4px;
        border-top-right-radius: 4px;
        border-bottom-left-radius: 0px;
        border-top-left-radius: 0px;
        padding: 8px;
    }
    .report-item-container .metadata {
        margin-top: 10px;
    }
    .report-item-container .metadata .row {
        margin-top: 5px;
    }
    .step-report .description {
        display: inline;
    }
    .context-item {
        border-radius: 5px;
        padding: 10px;
        margin-top: 10px;
    }

    .context-item-key {
        font-weight: bold;
    }

    .context-item-value {
        margin-top: 5px;
        padding: 5px;
        border: 1px solid #000000;
        font-family: "FreeMono";
        font-size: 12px;
    }
	    </style>
    </head>
    <body>
        <div class="title">${title}</div>
        <div class="step-report">
            <div class="section overview">
                <div class="section-title">
                    <div>${escape(overviewLabel)}</div>
                </div>
                <div class="section-content">
                    <table>
                        <tr>
                            <td class="cell-label">${escape(dateLabel)}</td>
                            <td class="cell-value">${reportDate}</td>
                        </tr>
                        <#if reportFileName??>
                            <tr>
                                <td class="cell-label">${escape(fileNameLabel)}</td>
                                <td class="cell-value">${reportFileName}</td>
                            </tr>
                        </#if>
                        <tr>
                            <td class="cell-label">${escape(resultFindingsLabel)}</td>
                            <td class="cell-value">${escape(resultFindingsDetailsLabel)}</td>
                        </tr>
                        <tr>
                            <td class="cell-label">${escape(resultLabel)}</td>
                            <td class="cell-value"><div class="value-inline result background-${reportResult}">${escape(resultTypeLabel)}</div></td>
                        </tr>
                    </table>
                </div>
            </div>
            <#if reportItems??>
                <div class="section details">
                    <div class="section-title">
                        <div>${escape(detailsLabel)}</div>
                    </div>
                    <div class="section-content">
                        <#list reportItems as item>
                            <div class="report-item background-strong-${item.level}">
                                <div class="report-item-container background-${item.level} border-${item.level}">
                                    <div class="row">
                                        <div class="icon"><img src="classpath:reports/images/${item.level}.svg"/></div>
                                        <#if richTextReportItems>
                                            <div class="description">${item.description}</div>
                                        <#else>
                                            <div class="description">${escape(item.description)}</div>
                                        </#if>
                                    </div>
                                    <#if item.location?? || item.test?? || item.assertionId??>
                                        <div class="metadata">
                                            <#if item.test??>
                                                <div class="row">
                                                    <div class="label">${escape(testLabel)}</div>
                                                    <div class="value-inline">${escape(item.test)}</div>
                                                </div>
                                            </#if>
                                            <#if item.location??>
                                                <div class="row">
                                                    <div class="label">${escape(locationLabel)}</div>
                                                    <div class="value-inline">${escape(item.location)}</div>
                                                </div>
                                            </#if>
                                            <#if item.assertionId??>
                                                <div class="row">
                                                    <div class="label">${escape(assertionIdLabel)}</div>
                                                    <div class="value-inline">${escape(item.assertionId)}</div>
                                                </div>
                                            </#if>
                                        </div>
                                    </#if>
                                </div>
                            </div>
                        </#list>
                    </div>
                </div>
            </#if>
        </div>
    </body>
</html>
