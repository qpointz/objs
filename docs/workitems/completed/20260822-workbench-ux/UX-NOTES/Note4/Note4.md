
I want to change layout of all graph bound top level views. 
- currently view level action buttons are above "graph context" which is bit illogical actions has to be shown within (visually below) graph or selection context 
- view  title redundant to top 

This change applies for **Explorer**, **Objects**, **Query** and **Composer** views
![[Pasted image 20260821151220.png]]

## Changes

### Explorer, Query, Objects 

**(1)**. Graph Context must be first row in view . 

**(2), (3)** Remove Title and doc icon (makes no sense, redundunt to product tour). Action buttons 
goes as separate row after graph context. 

### Composer
![[Pasted image 20260821151614.png]]

**(1)** block goes as first row, same logic as with graph context view , first row shows what is currently edited

**(2)**, **(3)**  remove title and doc. action buttons goes after **(1)**

### General changes 

Make sure action buttons across all affected views has same size across all views. Size must be same for buttons on same levels . e.g. view action buttons . second level buttons . etc. 
For this change review ONLY view level buttons. 


## Explorer changes

### type selector and action buttons 

![[Pasted image 20260821160203.png]]

Let's make block **(1)** aligned vertically to **(2)** block (1) content can fill content up to buttons, if there is more types it can be multi row. 

### right pane

make vertical splitter to resize right pane 


## Explorer type selection 

You applied change before to not apply transparency on type pills when type filtering applied . and it works great but it has side effect . Node of not selected type not transparent anymore . 
I want it to be independent . 

![[Pasted image 20260821165510.png]]

**(1)** - selected pill OK. keep it 
**(2)** - not selected pill OK, keep it.
**(3)** - graph content . apply transparency to nodes of types which is not selected. edges also must be transparent unless at least on participating node not transparent (has type in pill selection)