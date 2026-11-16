// url=https://www.figma.com/design/Rezbh6zkK0zjXbN9eXt7kJ/Untitled?node-id=0-1
// source=src/components/Button.tsx
// component=Button
import figma from 'figma'

const instance = figma.selectedInstance

// ---- Figma properties ----
const label = instance.getString('Label')
const variant = instance.getEnum('Variant', {
  'Primary': 'primary',
  'Secondary': 'secondary',
})
const size = instance.getEnum('Size', {
  'Small': 'sm',
  'Medium': 'md',
  'Large': 'lg',
})
const disabled = instance.getBoolean('Disabled')
const hasIcon = instance.getBoolean('Has Icon')
const iconInst = hasIcon ? instance.getInstanceSwap('Icon') : null
let iconCode: any = null
if (iconInst && iconInst.type === 'INSTANCE') {
  iconCode = iconInst.executeTemplate().example
}

export default {
  example: figma.code`
    <Button
      variant="${variant}"
      size="${size}"
      ${disabled ? 'disabled' : ''}
      ${iconCode ? figma.code`icon={${iconCode}}` : ''}
    >
      ${label}
    </Button>
  `,
  imports: ['import { Button } from "src/components/Button"'],
  id: 'button',
  metadata: { nestable: true },
}

