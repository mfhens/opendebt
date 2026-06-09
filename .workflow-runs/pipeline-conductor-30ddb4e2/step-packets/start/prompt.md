# Step Packet: start

## Node
- ID: start
- Label: Start
- Agent: pipeline-conductor
- Kind: execution
- Workflow origin: bundled


## Task
Acknowledge the workflow start and emit the status that advances into the first routed phase.

## Accepted statuses
- success

## Routing
- `success` -> `validate_inputs`

## Scope
Operate only on inputs provided and repository evidence relevant to this node.
If a workflow contract file is included in this packet, read it before deciding the result.
When routing rules describe fallback conditions such as "any status except success", emit the concrete status token that matches the workflow contract instead of the condition expression itself.

Do not modify other state unless the node instructions require repository changes.
