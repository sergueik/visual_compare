#Copyright (c) 2018 Serguei Kouzmine
#
#Permission is hereby granted, free of charge, to any person obtaining a copy
#of this software and associated documentation files (the "Software"), to deal
#in the Software without restriction, including without limitation the rights
#to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#copies of the Software, and to permit persons to whom the Software is
#furnished to do so, subject to the following conditions:
#
#The above copyright notice and this permission notice shall be included in
#all copies or substantial portions of the Software.
#
#THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
#THE SOFTWARE.

# use Powershell extract the download link of the latest available windows Image Magick distribution.
$url = 'https://www.imagemagick.org/script/download.php'

# based on https://gallery.technet.microsoft.com/Powershell-Tip-Parsing-49eb8810
# https://www.w3.org/TR/DOM-Level-2-HTML/html

$request = invoke-webrequest $url
$doc = $request.parsedHtml
$cached_content = "${env:TEMP}\content.html"
# caching $content in the file
$request.toString() |out-file $cached_content
$Content =  get-content -path $cached_content -raw
# $methods = ($html_file_obj | get-member |format-list )
# $methods | out-file -filepath 'methods.txt'

$html_file_obj1 = new-object -COM 'HTMLFile'
$html_file_obj1.IHTMLDocument2_write($content)
$node = $html_file_obj1.getElementById('windows').parentNode
# $node.outerHTML
# skip few paragraphs after the anchored header 'Windows Binary Release'  until the table with download links.
# <div class="table-responsive">

$sibling_node = $node.nextSibling
while ($sibling_node.nodeName -ne 'DIV') {
  $sibling_node = $sibling_node.nextSibling
}
# TODO: locate alternate Windows binary distributions
# process
$table_node = $sibling_node
# $table_node.innerHTML
$html_file_obj2 = new-object -COM 'HTMLFile'
$html_file_obj2.IHTMLDocument2_write($table_node.innerHTML)
$html_file_obj2.IHTMLDocument3_getElementsByTagName('a') | foreach-object {
  write-output $_.getAttribute('href')
  # write-output $_.Text
}
