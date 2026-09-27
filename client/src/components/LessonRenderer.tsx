import type { ContentBlock } from '../utils/api-types'
import { CodeBlock } from './blocks/CodeBlock'
import { HeadingBlock } from './blocks/HeadingBlock'
import { MCQBlock } from './blocks/MCQBlock'
import { ParagraphBlock } from './blocks/ParagraphBlock'
import { VideoBlock } from './blocks/VideoBlock'

interface LessonRendererProps {
  content: ContentBlock[]
}

export function LessonRenderer({ content }: LessonRendererProps) {
  return (
    <div>
      {content.map((block, index) => {
        switch (block.type) {
          case 'heading':
            return <HeadingBlock key={index} text={block.text as string} />
          case 'paragraph':
            return <ParagraphBlock key={index} text={block.text as string} />
          case 'code':
            return <CodeBlock key={index} language={block.language as string} text={block.text as string} />
          case 'video':
            return (
              <VideoBlock
                key={index}
                query={block.query as string}
                embedUrl={block.embedUrl as string | undefined}
              />
            )
          case 'mcq':
            return (
              <MCQBlock
                key={index}
                question={block.question as string}
                options={block.options as string[]}
                answer={block.answer as number}
                explanation={block.explanation as string}
              />
            )
          default:
            return null
        }
      })}
    </div>
  )
}
