Main change driver to get consistent experience functional and visual across all views in workbench.

![[Note-1-Pic 1.png|700]]


# Single context for Explore, Objects, Query

What i found silly in the moment , that context of Explore **(1)**, Objects **(2)** and Query **(3)** is independent. For every view independend "input" is used open graph, matcher expressions etc.

I want single "graph context" => (opened graph, or matcher expression) seamlessly used across these 3 views. User can change "graph context" in any view but once it is changed it is "reused in every view". Composer and Schema staying unbound to "graph context".

**(v1 naming)** Stick with **graph context** — no Workspace rename in v1.

Blocks **(4)** and **(5)** must be redisigned into slimmer "graph context" component to open context and display current selection. In the moment these two taking too much space.

Graph context must be placed in same position across Explore, Object, Query view . such that transition between views doesn't change position and state of "graph context". User must have feeling view changed context stays same.

Graph context is not only opened graph . it can also be "Obj-expr" (Selection today). "graph context" is not necessary graph entity it is source of entities and edges used for the view

It can also be set of graphs (e.g. similiar to SBOM example graph compositions).

Probably "graph context" under the hood graph context is chained expression. If it is appropriate solution. "Graph context" must visualy represent each component.

One of oprtion i see "graph-context" has **minimized** view . showing summary of components , total nodes edges. etc. **expanded** view where each component shown , components can be deleted added . context can be reseted etc.
Important collapsed is compact 2 -3 lines max allocating component. it must not distract viee content
"Expanded" - can be modal. or view in view => Expand brings into "context edit" view . Apply returns to view for where COntext expand view was called.
Details of graph context: see **## Graph context** below (**G-UX-ctx** resolved).
for v1. we can stick to single graph

Would be nice if there is option to store context history in the local store. no need to store every context context change. as option there is new / save (with name of context)/recent (list) button + storing last context across UI sessions. (deferred **G-UX-hist**)

### Also top level navigation to reordered

**(1)** **(2)** **(3)** - Explore, Object, Query . Groups views sharing context, also this group (virtual) groups read-only views. in fact it is Visual Exploration, Inspecting objects, and Query by traversals.

Compose and Schema goes as last views.

## **(1)** Explorer

General functionality remains the same no changes needed. Few improvements.

### Limit objects in graph view
i want to disable graph view if there are too many objects in context (we can stick  initially to 300 nodes)

### **(6) Object view**
must be extended. to show object version or (LATEST) (see recent versioning implementation).
History view must be integrated into obeject view. I want user has option to get list of object/edge versions. and inspect content of version. I would avoid populating list of versions when object selected. this will slowdown UI . May be link "versions" (next to selected version) to special view dialog to explore versions. Visually similar for nodes and edges; shared logic; not required to be one component (**G-UX-objver**).

## **(2)** Objects view
![[Note-1-Pic2.png]]As described above this view must share "graph context" with explorer view. Matcher expression **(1)** executes as chained within current context. which makes "obj-exp" is only option to use.

For better user experience i want **Shelf (2)** and **Matcher (1)**   placed in right pane . As tabbed content Tab for Matcher and Shelf . Put vertical splitter between right pane and content.

**(3)** action buttons should move into Shelf tab . explicitly bounding this actions to shelf . Same for "Search"  in matcher . Goes into **Matcher tab**.

## Query

![[Note-1-Pic 3.png]]Main change around graph-context same logic as for explorer and objects view -
Options should be moved right side (tab) (same logic as object ).
**Matcher on Query no longer makes sense** — only means of querying is the Query itself. Drop Query Matcher tab (**G-UX-q**).

## Graph context

This version will be only two option available resembling current setup . Open existing graph or use matcher expression . 

### graph is opened. 
![[Note-1-Pic4.png]]
1 - graph icon indicates that context is graph based 
2 - graph uuid , and copy to buffer
3- graph annotations (keep it small)
4- stats of count of nodes and edges in current graph 
5- "Open" split button with menu "Graph", "Matcher".
	Graph opens , graph open dialog (As today )
	Matcher , what we have today in selection tab of explorer 	

Colors in pic ONLY for demonstration **color schema must mutch rest of application . i want whit/black/blue schema as for rest of UI (exception is annotations)

### Matcher selection is opened
![[Note-1-Pic5.png]]1 - selection icon indicates context is selection based
2 - selection expression (can be truncated) 
3 - copy to buffer (expression)
4- nodes edges statistic
5- open (same as graph mode)