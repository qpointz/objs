
This note specify redwork of object details viewer

## Node view

![[Pasted image 20260821172642.png]]

**(1)**  remove completely 
**(2)** title, check logic currently it uses name . make sure it has fallback . i would use "typename-truncated id" e.g. `product-ea85d`**

I want object viewer to be splited into sections . Use section divider as below
![[Pasted image 20260821173715.png]] (same used in sbom example app viewer)

Each section begins with section divider 

### Section "Node"

**(3)**  this to be formatted as other places in UI . first `type` label to be formatted same  payload key labels  . 
Content should be `typename@schema version` => link to schema view 

(4) `id` full id and copy id button 

Do not use : between label and values , use same layout principle as in payload section 
### Section "Payload"
**(6)**  keep content as is, remove outer box 

### Section "Annotations"
**(7)** remove outer box , render full annotations list as key value instead of pills . use same layout as Payload section 
### Section "Versions"

**(5)** remove . implement as dedicated versions section 

first element of section content 
`version  object version` or LATEST . use layout as payload elements 

Next element of section content , must be lazy fetched when object selected. show skeleton as loading progress. You can implement endpoint to provide "versions statistic". 
Returning total number of versions and most recent N versions 

I would stick N = to be 5 for v1 

Add "versions N" element showing total versions . it must be link to version inspection dialog

Next goes list of  N most recent versions . Use version rendering as in "graph version selector"

```
Version     <grey> date </grey>
<small pills>annotations max 3
```

When clicked on version it goes to version inspection dialog. 

### Version inspection dialog

I want structure of version inspection dialog like 

![[Pasted image 20260821180626.png]]

It is similar to graph version selection dialog. 

**(1)** must be under 2 and stay there as first irrespective of selection in **(2)** or propose better solution
**(2)** make bit smaller as in graph there is too many space after AM/PM selector . this element must be top row of left side
**(3)** when no filter selected list shows 10 most recent 
**(4)** if version selected , or dialog opened for version (e.g. from object viewer ) put version descriptor as first element in 4. when no version selector . keep space of version descriptor . witj "select version" message- 

Content part of **(4)** is exactly same as Object viewer in explorer except `Version` section must be hidden. 

*IDEA READ CAREFULLY* May be better approach do not make version inspector popup dialog. but when `versions` link clicked in object viewer . `left side`  from this section . appear left side of object viewer . 
this will let to avoid blocking modal dialog . 

## Edge view 

Same as node view except . ![[Pasted image 20260821185609.png]]

- Additional Section `Relation` goes BEFORE payload .  Content of **(1)** goes into section. 
- Annotations irrelevant for the graph 

## Graph view 

It doesn't exists in the moment . Graph view must be shown when user clicks on canvas if graph and nothing is selected. 
Copy **node view** behavior. versions will be used to inspect version metadata. 

## General consideration 
I want implement Object viewer as component to reuse and provide same functionality across all views to deliver same experience 
In different context it may have different set of subviews . e.g. versions are disabled, no graph view etc. no need to implement it now. Just develop it such it will be easy to customize going forward 

