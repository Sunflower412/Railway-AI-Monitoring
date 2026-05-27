import React, { useState } from 'react';

const MIN_VISIBLE_CONFIDENCE = 0.01;

const AnnotatedMedia = ({ src, detections = [], alt = 'AI analysis result' }) => {
  const [imageSize, setImageSize] = useState({ width: 0, height: 0 });
  const validDetections = detections.filter((box) => (
    box &&
    Number.isFinite(Number(box.x)) &&
    Number.isFinite(Number(box.y)) &&
    Number.isFinite(Number(box.width)) &&
    Number.isFinite(Number(box.height)) &&
    Number(box.width) > 0 &&
    Number(box.height) > 0
  ));

  if (!src) {
    return null;
  }

  return (
    <div className="annotated-media">
      <img
        className="annotated-media__image"
        src={src}
        alt={alt}
        onLoad={(event) => {
          setImageSize({
            width: event.currentTarget.naturalWidth,
            height: event.currentTarget.naturalHeight,
          });
        }}
      />
      {imageSize.width > 0 && imageSize.height > 0 && validDetections.map((box, index) => {
        const confidence = Number(box.confidence || 0);
        const label = `${box.objectType || 'object'} ${Math.round(confidence * 100)}%`;

        return (
          <div
            key={`${box.objectType}-${index}-${box.x}-${box.y}`}
            className="annotated-media__box"
            style={{
              left: `${(Number(box.x) / imageSize.width) * 100}%`,
              top: `${(Number(box.y) / imageSize.height) * 100}%`,
              width: `${(Number(box.width) / imageSize.width) * 100}%`,
              height: `${(Number(box.height) / imageSize.height) * 100}%`,
            }}
          >
            {confidence >= MIN_VISIBLE_CONFIDENCE && (
              <span className="annotated-media__label">{label}</span>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default AnnotatedMedia;
