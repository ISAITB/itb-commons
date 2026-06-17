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
        margin-top: 30px;
        margin-bottom: 30px;
        background-color: #ededed;
        padding: 10px;
        border-radius: 5px;
        border: 1px solid #000000;
        position: relative;
        page-break-inside: avoid;
    }
    .section-title-text {
        display: inline-block;
        margin-right: 30px;
        width: 90%;
    }
    .section-title-link {
        margin-top: 2px;
        margin-right: 10px;
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
        white-space: nowrap
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
        margin-top: -3px;
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
    .findings-summary {
        border: 1px solid #000000;
        border-radius: 5px;
        background-color: #ededed;
    }
    .findings-summary td.cell-label {
        font-weight: normal;
    }
    .findings-summary table {
        width: 100%;
    }
    .findings-summary td.cell-label.total, .findings-summary td.cell-value.total {
        border-top: 1px solid #000000;
    }
    .findings-summary .cell-label-content {
        padding-left: 20px;
    }
    .findings-summary .cell-value-content {
        padding-right: 30px;
    }
    .findings-summary td.cell-label.total {
        font-weight: bold;
    }
    .custom-message-container {
        padding: 15px;
    }
	    </style>
    </head>
    <body>
        <div id="top" class="title">${title}</div>
        <div class="step-report">
            <div class="section overview">
                <#if customMessageOverview??><div class="custom-message-container">${customMessageOverview}</div></#if>
                <div class="section-title">
                    <div>${escape(overviewLabel)}</div>
                </div>
                <div class="section-content">
                    <table>
                      <tr>
                        <td style="width: 100%">
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
                                  <td class="cell-label">${escape(validationTypeLabel)}</td>
                                  <td class="cell-value">${escape(validationTypeName)}</td>
                              </tr>
                              <tr>
                                  <td class="cell-label">${escape(resultLabel)}</td>
                                  <td class="cell-value"><div class="value-inline result background-${reportResult}">${escape(resultTypeLabel)}</div></td>
                              </tr>
                          </table>
                        </td>
                        <td style="width: 1px;white-space: nowrap">
                          <div class="findings-summary">
                              <table>
                                <tr><#t>
                                    <td class="cell-label"><div class="cell-label-content"><#if errorItems??><a class="page-link" href="#section-errors">${escape(errorsLabel)}</a><#else>${escape(errorsLabel)}</#if></div></td><#t>
                                    <td class="cell-value"><div class="cell-value-content">${errorCount}</div></td><#t>
                                  </td><#t>
                                </tr><#t>
                                <tr><#t>
                                    <td class="cell-label"><div class="cell-label-content"><#if warningItems??><a class="page-link" href="#section-warnings">${escape(warningsLabel)}</a><#else>${escape(warningsLabel)}</#if></div></td><#t>
                                    <td class="cell-value"><div class="cell-value-content">${warningCount}</div></td><#t>
                                  </td><#t>
                                </tr><#t>
                                <tr><#t>
                                    <td class="cell-label"><div class="cell-label-content"><#if messageItems??><a class="page-link" href="#section-messages">${escape(messagesLabel)}</a><#else>${escape(messagesLabel)}</#if></div></td><#t>
                                    <td class="cell-value"><div class="cell-value-content">${messageCount}</div></td><#t>
                                  </td><#t>
                                </tr><#t>
                                <tr>
                                  <td class="cell-label total"><div class="cell-label-content">${escape(totalFindingsLabel)}</div></td>
                                  <td class="cell-value total"><div class="cell-value-content">${totalCount}</div></td>
                                </tr>
                              </table>
                          </div>
                        </td>
                      </tr>
                    </table>
                </div>
            </div>
            <#if errorItems??>
              <@itemSection title=errorSectionTitle reportItems=errorItems anchor="errors" message=customMessageErrors/>
            </#if>
            <#if warningItems??>
              <@itemSection title=warningSectionTitle reportItems=warningItems anchor="warnings" message=customMessageWarnings/>
            </#if>
            <#if messageItems??>
              <@itemSection title=messageSectionTitle reportItems=messageItems anchor="messages" message=customMessageMessages/>
            </#if>
        </div>
    </body>
</html>
<#macro subTitle text link="">
    <div class="sub-title">
        <div class="sub-title-text">${escape(text)}</div>
        <#if link != "">
            <div class="sub-title-link"><a href="#${link}">Top</a></div>
        </#if>
    </div>
</#macro>
<#macro itemSection title reportItems anchor message="">
    <div id="section-${anchor}" class="section"><#t>
        <div class="section-title">${escape(title)}</div><#t>
        <#if message?has_content><div class="custom-message-container">${message}</div></#if><#t>
        <div class="section-content"><#t>
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
</#macro>